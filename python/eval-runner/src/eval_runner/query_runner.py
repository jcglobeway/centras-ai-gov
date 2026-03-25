"""
RAG 질의 실행기.

eval_questions.json을 읽어 admin-api에 실제 질의를 보내고,
RAG 검색 로그로 contexts를 수집해 eval_results.json을 생성한다.

실행: python -m eval_runner.query_runner
     python -m eval_runner.query_runner --input eval_questions.json --limit 20
"""
from __future__ import annotations

import json
import os
import time
from pathlib import Path
from typing import Optional

import httpx
import psycopg2
import typer

app = typer.Typer(help="RAG 질의 실행 및 결과 수집")

_PROJECT_ROOT  = Path(__file__).parents[4]  # centras-ai-gov/
DEFAULT_INPUT  = _PROJECT_ROOT / "python" / "eval-runner" / "eval_questions.json"
DEFAULT_OUTPUT = _PROJECT_ROOT / "python" / "eval-runner" / "eval_results.json"


# ── Admin API 클라이언트 ──────────────────────────────────────────────────────

class AdminApiClient:
    def __init__(self, base_url: str, session_token: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.headers = {"X-Admin-Session-Id": session_token, "Content-Type": "application/json"}

    # org별 eval 세션 ID 매핑 (reset_data 실행 후 DB에 직접 삽입된 더미 세션)
    _EVAL_SESSIONS: dict = {
        "org_acc":         "eval_session_acc",
        "org_local_gov":   "eval_session_local",
        "org_central_gov": "eval_session_central",
    }
    _DEFAULT_SESSION = "eval_session_acc"

    def create_question(
        self,
        org_id: str,
        service_id: str,
        question_text: str,
        question_category: Optional[str] = None,
        question_intent_label: Optional[str] = None,
    ) -> Optional[str]:
        """POST /admin/questions → question_id 반환."""
        session_id = self._EVAL_SESSIONS.get(org_id, self._DEFAULT_SESSION)
        payload = {
            "organizationId": org_id,
            "serviceId": service_id,
            "chatSessionId": session_id,
            "questionText": question_text,
            "channel": "api",
            "questionCategory": question_category,
            "questionIntentLabel": question_intent_label,
        }
        try:
            resp = httpx.post(
                f"{self.base_url}/admin/questions",
                json=payload, headers=self.headers, timeout=30.0,
            )
            resp.raise_for_status()
            return resp.json().get("questionId")
        except Exception as e:
            typer.echo(f"[query] 질의 생성 실패: {e}", err=True)
            return None

    def fetch_answer_text(self, question_id: str, db_url: Optional[str]) -> str:
        """answers 테이블에서 answer_text를 직접 조회한다 (GET /admin/questions/{id} 미구현)."""
        if not db_url:
            return ""
        try:
            conn = psycopg2.connect(db_url)
            cur = conn.cursor()
            cur.execute(
                "SELECT answer_text FROM answers WHERE question_id = %s LIMIT 1",
                (question_id,),
            )
            row = cur.fetchone()
            cur.close()
            conn.close()
            return row[0] if row else ""
        except Exception as e:
            typer.echo(f"[query] DB 답변 조회 실패: {e}", err=True)
            return ""

    def fetch_rag_search_logs(self, question_id: str) -> list[str]:
        """GET /admin/rag-search-logs?question_id= → retrieved document 텍스트 목록."""
        try:
            resp = httpx.get(
                f"{self.base_url}/admin/rag-search-logs",
                params={"question_id": question_id},
                headers=self.headers, timeout=10.0,
            )
            if resp.status_code != 200:
                return []
            data = resp.json()
            logs = data.get("items", data) if isinstance(data, dict) else data
            contexts: list[str] = []
            for log in logs:
                for doc in log.get("retrievedDocuments", []):
                    chunk_text = doc.get("chunkText") or doc.get("content") or ""
                    if chunk_text:
                        contexts.append(chunk_text)
            return contexts
        except Exception as e:
            typer.echo(f"[query] RAG 로그 조회 실패: {e}", err=True)
            return []


# ── 메인 커맨드 ───────────────────────────────────────────────────────────────

@app.command()
def run(
    input_path: Path = typer.Option(DEFAULT_INPUT, "--input", help="eval_questions.json 경로"),
    output_path: Path = typer.Option(DEFAULT_OUTPUT, "--output", help="eval_results.json 출력 경로"),
    limit: int = typer.Option(100, "--limit", help="처리할 최대 건수"),
    delay: float = typer.Option(0.5, "--delay", help="질의 간 대기(초) — API 부하 방지"),
    dry_run: bool = typer.Option(False, "--dry-run", help="실제 API 호출 없이 구조만 출력"),
) -> None:
    """eval_questions.json을 읽어 RAG 질의를 실행하고 eval_results.json을 생성한다."""
    admin_api_url   = os.getenv("ADMIN_API_BASE_URL",    "http://localhost:8081")
    session_token   = os.getenv("ADMIN_API_SESSION_TOKEN", "")
    db_url          = os.getenv("DATABASE_URL")

    if not input_path.exists():
        typer.echo(f"[query] 입력 파일 없음: {input_path}", err=True)
        typer.echo("  → ingestion_prep.py를 먼저 실행하세요.", err=True)
        raise typer.Exit(1)

    questions: list[dict] = json.loads(input_path.read_text(encoding="utf-8"))
    questions = questions[:limit]
    typer.echo(f"[query] {len(questions)}건 처리 시작 (admin-api: {admin_api_url})")

    client = AdminApiClient(base_url=admin_api_url, session_token=session_token)
    results: list[dict] = []

    for i, item in enumerate(questions, 1):
        q_text   = item["question"]
        org_id   = item.get("org_id", "org_acc")
        svc_id   = item.get("service_id", "svc_acc_chatbot")
        gt       = item.get("ground_truth", "")
        task_cat = item.get("task_category", "")

        typer.echo(f"[query] [{i}/{len(questions)}] {q_text[:50]}...")

        if dry_run:
            results.append({
                "question": q_text,
                "answer":   "(dry-run)",
                "contexts": [],
                "ground_truth": gt,
                "task_category": task_cat,
                "question_id": None,
            })
            continue

        question_id = client.create_question(
            org_id, svc_id, q_text,
            question_category=item.get("consulting_category"),
            question_intent_label=item.get("task_category"),
        )
        if not question_id:
            results.append({
                "question": q_text, "answer": "", "contexts": [],
                "ground_truth": gt, "task_category": task_cat, "question_id": None,
            })
            continue

        # 답변이 비동기 생성되므로 짧게 대기
        time.sleep(max(delay, 1.0))

        answer   = client.fetch_answer_text(question_id, db_url)
        contexts = client.fetch_rag_search_logs(question_id)

        results.append({
            "question":      q_text,
            "answer":        answer,
            "contexts":      contexts if contexts else [item.get("context", "")],
            "ground_truth":  gt,
            "task_category": task_cat,
            "question_id":   question_id,
        })

        if delay > 0:
            time.sleep(delay)

    output_path.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    typer.echo(f"[query] 완료 → {output_path} ({len(results)}건)")


def main() -> None:
    from dotenv import load_dotenv
    load_dotenv()
    app()


if __name__ == "__main__":
    main()
