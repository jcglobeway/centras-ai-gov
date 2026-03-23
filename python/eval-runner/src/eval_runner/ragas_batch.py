"""
오프라인 RAGAS 배치 평가 실행기.

admin-api에서 질문/답변 데이터를 조회한 뒤 RAGAS 지표(Faithfulness,
AnswerRelevancy)를 계산하고 결과를 admin-api /admin/ragas-evaluations로 전송한다.

실행: eval-runner --date 2026-03-20
     eval-runner --date 2026-03-20 --organization-id org_seoul_120
"""
from __future__ import annotations

import math
import os
from typing import Optional

import httpx
import psycopg2
import psycopg2.extras
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

    # DB에서 직접 질문+답변 쌍 조회 (API에 answerText 미포함 문제 우회)
    db_url = os.getenv("DATABASE_URL")
    if db_url:
        questions = fetch_questions_from_db(db_url, date_str, organization_id, page_size)
    else:
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
        params: dict = {"from_date": from_date, "to_date": to_date, "page_size": page_size}
        if organization_id:
            params["organization_id"] = organization_id
        try:
            response = httpx.get(
                f"{self.base_url}/admin/questions",
                params=params, headers=self.headers, timeout=10.0,
            )
            response.raise_for_status()
            return response.json().get("items", [])
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

def fetch_questions_from_db(
    db_url: str,
    date_str: str,
    organization_id: Optional[str],
    page_size: int,
) -> list[dict]:
    """PostgreSQL에서 직접 질문+답변 쌍을 조회한다."""
    try:
        conn = psycopg2.connect(db_url)
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        query = """
            SELECT q.id AS "questionId", q.question_text AS "questionText",
                   a.answer_text AS "answerText", a.answer_status AS "answerStatus"
            FROM questions q
            LEFT JOIN answers a ON a.question_id = q.id
            WHERE DATE(q.created_at) = %s
              AND a.answer_text IS NOT NULL AND a.answer_text != ''
              {org_filter}
            LIMIT %s
        """.format(org_filter="AND q.organization_id = %s" if organization_id else "")
        params = [date_str, organization_id, page_size] if organization_id else [date_str, page_size]
        cur.execute(query, params)
        rows = cur.fetchall()
        cur.close()
        conn.close()
        return [dict(r) for r in rows]
    except Exception as e:
        typer.echo(f"[eval-runner] DB 조회 실패: {e}", err=True)
        return []


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
    judge_model = os.getenv("RAGAS_OLLAMA_MODEL", "qwen2.5:7b")
    ollama_url = os.getenv("OLLAMA_URL", "http://jcg-office.tailedf4dc.ts.net:11434")
    try:
        from ragas.metrics import faithfulness, answer_relevancy
        from ragas import evaluate
        from ragas.llms import LangchainLLMWrapper
        from ragas.embeddings import LangchainEmbeddingsWrapper
        from langchain_ollama import ChatOllama, OllamaEmbeddings
        from datasets import Dataset

        llm = LangchainLLMWrapper(ChatOllama(base_url=ollama_url, model=judge_model))
        emb = LangchainEmbeddingsWrapper(OllamaEmbeddings(base_url=ollama_url, model="bge-m3"))
        faithfulness.llm = llm
        answer_relevancy.llm = llm
        answer_relevancy.embeddings = emb

        dataset = Dataset.from_dict({
            "question": [question_text],
            "answer": [answer_text],
            "contexts": [[""]],
        })
        result = evaluate(dataset, metrics=[faithfulness, answer_relevancy])
        scores = result.to_pandas().iloc[0].to_dict()

        def _safe(v: object) -> "float | None":
            try:
                f = float(v)  # type: ignore[arg-type]
                return None if math.isnan(f) else f
            except (TypeError, ValueError):
                return None

        return {
            "questionId": question_id,
            "faithfulness": _safe(scores.get("faithfulness")),
            "answerRelevancy": _safe(scores.get("answer_relevancy")),
            "contextPrecision": None,
            "contextRecall": None,
            "judgeProvider": "ollama",
            "judgeModel": judge_model,
        }
    except Exception as e:
        typer.echo(f"[eval-runner] 평가 실패 ({question_id}): {e}", err=True)
        return {
            "questionId": question_id,
            "faithfulness": None,
            "answerRelevancy": None,
            "contextPrecision": None,
            "contextRecall": None,
            "judgeProvider": "ollama",
            "judgeModel": judge_model,
        }


if __name__ == "__main__":
    app()
