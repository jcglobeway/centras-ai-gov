"""
오프라인 RAGAS 배치 평가 실행기.

admin-api에서 질문/답변 데이터를 조회한 뒤 RAGAS 지표(Faithfulness,
AnswerRelevancy)를 계산하고 결과를 admin-api /admin/ragas-evaluations로 전송한다.

실행: eval-runner --date 2026-03-20
     eval-runner --date 2026-03-20 --organization-id org_seoul_120
"""
from __future__ import annotations

import os
from typing import Optional

import httpx
import typer

app = typer.Typer(help="RAGAS 배치 평가 실행기")


@app.command()
def run(
    date_str: str = typer.Option(..., "--date", help="평가 대상 날짜 (YYYY-MM-DD)"),
    organization_id: Optional[str] = typer.Option(None, "--organization-id", help="조직 ID (미입력 시 전체)"),
    page_size: int = typer.Option(50, "--page-size", help="배치 크기"),
    dry_run: bool = typer.Option(False, "--dry-run", help="결과를 전송하지 않고 출력만"),
) -> None:
    """지정한 날짜의 질문에 대해 RAGAS 평가를 수행하고 admin-api에 결과를 전송한다."""
    admin_api_url = os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")
    session_token = os.getenv("ADMIN_API_SESSION_TOKEN", "")

    typer.echo(f"[eval-runner] 평가 시작 — 날짜: {date_str}, 조직: {organization_id or '전체'}")

    client = AdminApiClient(base_url=admin_api_url, session_token=session_token)
    questions = client.fetch_questions(
        from_date=date_str, to_date=date_str,
        organization_id=organization_id, page_size=page_size,
    )

    if not questions:
        typer.echo("[eval-runner] 평가 대상 질문 없음")
        return

    typer.echo(f"[eval-runner] 평가 대상: {len(questions)}건")
    results = evaluate_batch(questions)

    for result in results:
        if dry_run:
            typer.echo(f"[DRY-RUN] {result}")
        else:
            client.post_evaluation(result)

    typer.echo(f"[eval-runner] 완료 — {len(results)}건 처리")


# ── Admin API 클라이언트 ──────────────────────────────────────────────────────

class AdminApiClient:
    def __init__(self, base_url: str, session_token: str) -> None:
        self.base_url = base_url
        self.headers = {"X-Admin-Session-Id": session_token}

    def fetch_questions(
        self,
        from_date: str,
        to_date: str,
        organization_id: Optional[str],
        page_size: int,
    ) -> list[dict]:
        params: dict = {"from": from_date, "to": to_date, "page_size": page_size}
        if organization_id:
            params["organization_id"] = organization_id
        try:
            response = httpx.get(
                f"{self.base_url}/admin/questions",
                params=params, headers=self.headers, timeout=10.0,
            )
            response.raise_for_status()
            return response.json().get("questions", [])
        except Exception as e:
            typer.echo(f"[eval-runner] 질문 조회 실패: {e}", err=True)
            return []

    def post_evaluation(self, result: dict) -> None:
        try:
            httpx.post(
                f"{self.base_url}/admin/ragas-evaluations",
                json=result, headers=self.headers, timeout=5.0,
            )
        except Exception as e:
            typer.echo(f"[eval-runner] 평가 결과 전송 실패: {e}", err=True)


# ── RAGAS 평가 로직 ───────────────────────────────────────────────────────────

def evaluate_batch(questions: list[dict]) -> list[dict]:
    return [
        _compute_metrics(
            question_id=q.get("questionId", ""),
            question_text=q.get("questionText", ""),
            answer_text=q.get("answerText", ""),
        )
        for q in questions
    ]


def _compute_metrics(question_id: str, question_text: str, answer_text: str) -> dict:
    judge_provider = os.getenv("RAGAS_JUDGE_PROVIDER", "ollama")
    judge_model = os.getenv("RAGAS_OLLAMA_MODEL", "qwen2.5:7b")
    try:
        from ragas.metrics import faithfulness, answer_relevancy
        from ragas import evaluate
        from datasets import Dataset

        dataset = Dataset.from_dict({
            "question": [question_text],
            "answer": [answer_text],
            "contexts": [[""]],
        })
        result = evaluate(dataset, metrics=[faithfulness, answer_relevancy])
        scores = result.to_pandas().iloc[0].to_dict()
        return {
            "questionId": question_id,
            "faithfulness": scores.get("faithfulness"),
            "answerRelevancy": scores.get("answer_relevancy"),
            "contextPrecision": None,
            "contextRecall": None,
            "judgeProvider": judge_provider,
            "judgeModel": judge_model,
        }
    except Exception:
        return {
            "questionId": question_id,
            "faithfulness": None,
            "answerRelevancy": None,
            "contextPrecision": None,
            "contextRecall": None,
            "judgeProvider": judge_provider,
            "judgeModel": judge_model,
        }


if __name__ == "__main__":
    app()
