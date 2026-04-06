"""
DB → eval_results.json 추출 스크립트.

ragas_evaluations가 없는 질문을 DB에서 읽어 eval_results.json을 재구성한다.
bulk_query_runner로 투입된 질문처럼 ground_truth 없이 투입된 경우에 사용한다.

실행:
  export-db-eval-data --org-id org_acc --limit 200
"""
from __future__ import annotations

import json
import os
from pathlib import Path

import httpx
import psycopg2
import typer

app = typer.Typer(help="DB → eval_results.json 재구성")

_PROJECT_ROOT = Path(__file__).parents[4]
DEFAULT_OUTPUT = _PROJECT_ROOT / "python" / "eval-runner" / "eval_results.json"


@app.command()
def run(
    org_id: str = typer.Option("org_acc", "--org-id"),
    limit: int = typer.Option(200, "--limit", help="추출할 최대 건수"),
    skip_evaluated: bool = typer.Option(True, "--skip-evaluated/--no-skip-evaluated",
                                        help="이미 ragas_evaluations 있는 질문 제외"),
    output_path: Path = typer.Option(DEFAULT_OUTPUT, "--output"),
) -> None:
    """DB에서 미평가 질문을 읽어 eval_results.json을 생성한다."""
    db_url = os.getenv("DATABASE_URL")
    admin_url = os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081").rstrip("/")
    session_token = os.getenv("ADMIN_API_SESSION_TOKEN", "")

    if not db_url:
        typer.echo("[export] DATABASE_URL 환경변수가 없습니다.", err=True)
        raise typer.Exit(1)

    headers = {"X-Admin-Session-Id": session_token}

    conn = psycopg2.connect(db_url)
    cur = conn.cursor()

    skip_clause = """
        AND NOT EXISTS (
            SELECT 1 FROM ragas_evaluations re WHERE re.question_id = q.id
        )
    """ if skip_evaluated else ""

    cur.execute(f"""
        SELECT q.id, q.question_text, a.answer_text
        FROM questions q
        JOIN answers a ON a.question_id = q.id
        WHERE q.organization_id = %s
          AND a.answer_status = 'answered'
          {skip_clause}
        ORDER BY q.created_at DESC
        LIMIT %s
    """, (org_id, limit))

    rows = cur.fetchall()
    cur.close()
    conn.close()

    typer.echo(f"[export] {len(rows)}건 추출 (org={org_id}, skip_evaluated={skip_evaluated})")

    results: list[dict] = []
    for i, (question_id, question_text, answer_text) in enumerate(rows, 1):
        typer.echo(f"[export] [{i}/{len(rows)}] {question_text[:50]}...")

        contexts: list[str] = []
        try:
            resp = httpx.get(
                f"{admin_url}/admin/questions/{question_id}/context",
                headers=headers, timeout=10.0,
            )
            if resp.status_code == 200:
                for chunk in resp.json().get("retrievedChunks", []):
                    text = chunk.get("chunkText") or ""
                    if text:
                        contexts.append(text)
        except Exception as e:
            typer.echo(f"[export]   context 조회 실패: {e}", err=True)

        results.append({
            "question": question_text,
            "answer": answer_text or "",
            "contexts": contexts if contexts else [""],
            "ground_truth": "",
            "question_id": question_id,
        })

    output_path.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    typer.echo(f"[export] 완료 → {output_path} ({len(results)}건)")

    has_ctx = sum(1 for r in results if any(c for c in r["contexts"]))
    typer.echo(f"[export] contexts 있음: {has_ctx}/{len(results)}건")


def main() -> None:
    from pathlib import Path
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parents[3] / ".env", override=False)
    app()


if __name__ == "__main__":
    main()
