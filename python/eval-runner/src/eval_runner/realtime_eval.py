"""
Redis BRPOP으로 ragas:eval:queue를 구독하고,
새 answer가 생성될 때마다 자동으로 RAGAS 평가를 수행한다.
답변 생성 파이프라인과 완전히 분리된 비동기 프로세스.

실행: realtime-eval --redis-url redis://localhost:6379 --admin-api-url http://localhost:8081
"""
from __future__ import annotations

import json
import os
import signal
import sys
from typing import Optional

import httpx
import typer

app = typer.Typer(help="RAGAS 실시간 평가 데몬")

_running = True


def _handle_signal(signum: int, frame: object) -> None:
    global _running
    typer.echo("[realtime-eval] 종료 신호 수신 — 루프 종료 중...")
    _running = False


@app.command()
def run(
    redis_url: str = typer.Option(
        None, "--redis-url", envvar="REDIS_URL", help="Redis URL (예: redis://localhost:6379)"
    ),
    admin_api_url: str = typer.Option(
        None, "--admin-api-url", envvar="ADMIN_API_BASE_URL", help="Admin API 기본 URL"
    ),
    session_id: str = typer.Option(
        "", "--session-id", envvar="ADMIN_API_SESSION_TOKEN", help="Admin API 세션 토큰"
    ),
    brpop_timeout: int = typer.Option(5, "--timeout", help="BRPOP 타임아웃 (초)"),
) -> None:
    """ragas:eval:queue를 구독해 실시간 RAGAS 평가를 수행한다."""
    import redis as redis_lib

    redis_url = redis_url or os.getenv("REDIS_URL", "redis://localhost:6379")
    admin_api_url = admin_api_url or os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")

    typer.echo(f"[realtime-eval] 시작 — redis={redis_url}, admin-api={admin_api_url}")

    signal.signal(signal.SIGINT, _handle_signal)
    signal.signal(signal.SIGTERM, _handle_signal)

    client = redis_lib.from_url(redis_url, decode_responses=True)
    api = _AdminApiClient(base_url=admin_api_url, session_token=session_id)

    while _running:
        try:
            result = client.brpop("ragas:eval:queue", timeout=brpop_timeout)
            if result is None:
                continue

            _, payload_str = result
            payload = json.loads(payload_str)
            question_id = payload.get("questionId", "")
            organization_id = payload.get("organizationId")

            if not question_id:
                typer.echo("[realtime-eval] questionId 없는 메시지 — 스킵", err=True)
                continue

            typer.echo(f"[realtime-eval] 평가 시작 — questionId={question_id}")
            _process(api, question_id, organization_id)

        except redis_lib.RedisError as e:
            typer.echo(f"[realtime-eval] Redis 오류: {e}", err=True)
        except Exception as e:
            typer.echo(f"[realtime-eval] 처리 오류: {e}", err=True)

    typer.echo("[realtime-eval] 종료")


# ── 핵심 평가 흐름 ─────────────────────────────────────────────────────────────

def _process(api: "_AdminApiClient", question_id: str, organization_id: Optional[str]) -> None:
    import redis as redis_lib

    try:
        question = api.fetch_question(question_id)
        if question is None:
            typer.echo(f"[realtime-eval] 질문 조회 실패 — questionId={question_id}", err=True)
            _push_dlq(api, question_id, organization_id, "질문 조회 실패")
            return

        question_text = question.get("questionText", "")
        answer_text = question.get("answerText", "")

        if not answer_text:
            typer.echo(f"[realtime-eval] 답변 없음 — questionId={question_id}, 스킵")
            return

        contexts = api.fetch_contexts(question_id)

        from eval_runner.ragas_batch import _compute_metrics
        metrics = _compute_metrics(
            question_id=question_id,
            question_text=question_text,
            answer_text=answer_text,
            contexts=contexts if contexts else [""],
            ground_truth=None,
        )

        if organization_id:
            metrics["organizationId"] = organization_id

        api.post_evaluation(metrics)
        typer.echo(f"[realtime-eval] 평가 완료 — questionId={question_id}, faithfulness={metrics.get('faithfulness')}")

    except Exception as e:
        typer.echo(f"[realtime-eval] 평가 오류 — questionId={question_id}: {e}", err=True)
        _push_dlq(api, question_id, organization_id, str(e))


def _push_dlq(
    api: "_AdminApiClient",
    question_id: str,
    organization_id: Optional[str],
    reason: str,
) -> None:
    import redis as redis_lib

    try:
        redis_url = os.getenv("REDIS_URL", "redis://localhost:6379")
        client = redis_lib.from_url(redis_url, decode_responses=True)
        payload = json.dumps({"questionId": question_id, "organizationId": organization_id, "reason": reason})
        client.lpush("ragas:eval:dlq", payload)
    except Exception as e:
        typer.echo(f"[realtime-eval] DLQ push 실패: {e}", err=True)


# ── Admin API 클라이언트 ──────────────────────────────────────────────────────

class _AdminApiClient:
    def __init__(self, base_url: str, session_token: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.headers = {"X-Admin-Session-Id": session_token}

    def fetch_question(self, question_id: str) -> Optional[dict]:
        try:
            resp = httpx.get(
                f"{self.base_url}/admin/questions",
                params={"question_id": question_id, "page_size": "1"},
                headers=self.headers,
                timeout=10.0,
            )
            resp.raise_for_status()
            items = resp.json().get("items", [])
            return items[0] if items else None
        except Exception as e:
            typer.echo(f"[realtime-eval] 질문 조회 실패: {e}", err=True)
            return None

    def fetch_contexts(self, question_id: str) -> list[str]:
        try:
            resp = httpx.get(
                f"{self.base_url}/admin/questions/{question_id}/context",
                headers=self.headers,
                timeout=10.0,
            )
            resp.raise_for_status()
            data = resp.json()
            chunks = data.get("retrievedChunks", [])
            return [c.get("chunkText", "") for c in chunks if c.get("chunkText")]
        except Exception:
            return []

    def post_evaluation(self, payload: dict) -> None:
        try:
            httpx.post(
                f"{self.base_url}/admin/ragas-evaluations",
                json=payload,
                headers=self.headers,
                timeout=10.0,
            )
        except Exception as e:
            typer.echo(f"[realtime-eval] 평가 결과 전송 실패: {e}", err=True)


def main() -> None:
    from pathlib import Path
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parents[3] / ".env", override=False)
    app()


if __name__ == "__main__":
    main()
