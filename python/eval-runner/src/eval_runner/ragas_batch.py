"""
오프라인 RAGAS 배치 평가 실행기.

admin-api에서 질문/답변 데이터를 조회한 뒤 RAGAS 지표(Faithfulness,
AnswerRelevancy, ContextPrecision, ContextRecall)를 계산하고
결과를 admin-api /admin/ragas-evaluations로 전송한다.

기존 평가 행이 있으면 null 필드만 선택적으로 PATCH 하여 중복 행을 방지한다.
ContextPrecision·ContextRecall은 eval_results.json(query_runner 출력)의
contexts와 ground_truth를 사용한다.

실행: eval-runner --date 2026-03-20
     eval-runner --date 2026-03-20 --organization-id org_acc
"""
from __future__ import annotations

import json
import math
import os
from pathlib import Path
from typing import Optional

import httpx
import psycopg2
import psycopg2.extras
import typer

app = typer.Typer(help="RAGAS 배치 평가 실행기")


_PROJECT_ROOT = Path(__file__).parents[4]
DEFAULT_EVAL_RESULTS = _PROJECT_ROOT / "python" / "eval-runner" / "eval_results.json"


@app.command()
def run(
    date_str: Optional[str] = typer.Option(None, "--date", help="평가 대상 날짜 (YYYY-MM-DD). --from-eval-results 사용 시 생략 가능"),
    organization_id: Optional[str] = typer.Option(None, "--organization-id", help="조직 ID (미입력 시 전체)"),
    page_size: int = typer.Option(50, "--page-size", help="배치 크기"),
    eval_results_path: Path = typer.Option(DEFAULT_EVAL_RESULTS, "--eval-results", help="eval_results.json 경로 (contexts/ground_truth 보완용)"),
    from_eval_results: bool = typer.Option(False, "--from-eval-results", help="eval_results.json의 question_id 목록으로 직접 조회 (날짜 필터 무시)"),
    dry_run: bool = typer.Option(False, "--dry-run", help="결과를 전송하지 않고 출력만"),
) -> None:
    """지정한 날짜의 질문에 대해 RAGAS 평가를 수행하고 admin-api에 결과를 전송한다."""
    if not from_eval_results and not date_str:
        typer.echo("[eval-runner] --date 또는 --from-eval-results 중 하나를 지정해야 합니다.", err=True)
        raise typer.Exit(1)

    admin_api_url = os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")
    session_token = os.getenv("ADMIN_API_SESSION_TOKEN", "")

    client = AdminApiClient(base_url=admin_api_url, session_token=session_token)

    # eval_results.json 로드 (contexts·ground_truth 보완 + question_id 기반 조회에도 사용)
    eval_lookup: dict[str, dict] = {}  # question_text → item
    eval_by_id: dict[str, dict] = {}   # question_id → item
    eval_data: list[dict] = []
    if eval_results_path.exists():
        try:
            eval_data = json.loads(eval_results_path.read_text(encoding="utf-8"))
            for item in eval_data:
                q_text = item.get("question", "").strip()
                q_id = item.get("question_id", "")
                if q_text:
                    eval_lookup[q_text] = item
                if q_id:
                    eval_by_id[q_id] = item
            typer.echo(f"[eval-runner] eval_results.json 로드: {len(eval_data)}건 (question_id 있음: {len(eval_by_id)}건)")
        except Exception as e:
            typer.echo(f"[eval-runner] eval_results.json 로드 실패: {e}", err=True)

    db_url = os.getenv("DATABASE_URL")

    if from_eval_results:
        # eval_results.json의 question_id 목록으로 직접 DB 조회 (날짜 필터 무시)
        question_ids = [item["question_id"] for item in eval_data if item.get("question_id")]
        if not question_ids:
            typer.echo("[eval-runner] eval_results.json에 question_id가 없습니다.", err=True)
            raise typer.Exit(1)
        typer.echo(f"[eval-runner] 평가 시작 — eval_results.json 기반, {len(question_ids)}건")
        if not db_url:
            typer.echo("[eval-runner] --from-eval-results 모드는 DATABASE_URL이 필요합니다.", err=True)
            raise typer.Exit(1)
        questions = fetch_questions_by_ids(db_url, question_ids, page_size)
    else:
        typer.echo(f"[eval-runner] 평가 시작 — 날짜: {date_str}, 조직: {organization_id or '전체'}")
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

    # question_id 기준 lookup이 있으면 우선 사용, 없으면 question_text 기준 fallback
    merged_lookup: dict[str, dict] = {}
    for q in questions:
        qid = q.get("questionId", "")
        qtext = q.get("questionText", "")
        if qid and qid in eval_by_id:
            merged_lookup[qtext] = eval_by_id[qid]
        elif qtext in eval_lookup:
            merged_lookup[qtext] = eval_lookup[qtext]

    typer.echo(f"[eval-runner] 평가 대상: {len(questions)}건 (eval_results 매핑: {len(merged_lookup)}건)")
    results = evaluate_batch(questions, merged_lookup, client=client, dry_run=dry_run)

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

    def get_evaluation(self, question_id: str) -> "dict | None":
        try:
            response = httpx.get(
                f"{self.base_url}/admin/ragas-evaluations/by-question/{question_id}",
                headers=self.headers, timeout=5.0,
            )
            if response.status_code == 404:
                return None
            response.raise_for_status()
            return response.json()
        except Exception as e:
            typer.echo(f"[eval-runner] 평가 조회 실패 ({question_id}): {e}", err=True)
            return None

    def patch_evaluation(self, question_id: str, metrics: dict) -> None:
        try:
            httpx.patch(
                f"{self.base_url}/admin/ragas-evaluations/by-question/{question_id}",
                json=metrics, headers=self.headers, timeout=5.0,
            )
        except Exception as e:
            typer.echo(f"[eval-runner] 평가 PATCH 실패 ({question_id}): {e}", err=True)


# ── RAGAS 평가 로직 ───────────────────────────────────────────────────────────

def fetch_questions_by_ids(
    db_url: str,
    question_ids: list[str],
    page_size: int,
) -> list[dict]:
    """question_id 목록으로 직접 질문+답변을 조회한다 (날짜 필터 없음)."""
    results: list[dict] = []
    try:
        conn = psycopg2.connect(db_url)
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        # page_size 단위로 분할 조회 (IN 절 크기 제한 회피)
        for offset in range(0, len(question_ids), page_size):
            batch = question_ids[offset:offset + page_size]
            placeholders = ",".join(["%s"] * len(batch))
            cur.execute(f"""
                SELECT q.id AS "questionId", q.question_text AS "questionText",
                       a.answer_text AS "answerText", a.answer_status AS "answerStatus"
                FROM questions q
                LEFT JOIN answers a ON a.question_id = q.id
                WHERE q.id IN ({placeholders})
                  AND a.answer_text IS NOT NULL AND a.answer_text != ''
            """, batch)
            rows = cur.fetchall()
            results.extend([dict(r) for r in rows])
            typer.echo(f"[eval-runner] DB 조회: {offset + len(batch)}/{len(question_ids)}건 처리")
        cur.close()
        conn.close()
    except Exception as e:
        typer.echo(f"[eval-runner] DB 조회 실패: {e}", err=True)
    return results


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


def evaluate_batch(
    questions: list[dict],
    eval_lookup: dict[str, dict] | None = None,
    client: "AdminApiClient | None" = None,
    dry_run: bool = False,
) -> list[dict]:
    lookup = eval_lookup or {}
    results: list[dict] = []

    for q in questions:
        question_id = q.get("questionId", "")
        question_text = q.get("questionText", "")
        answer_text = q.get("answerText", "")
        eval_item = lookup.get(question_text, {})
        contexts = eval_item.get("contexts") or [""]
        ground_truth = eval_item.get("ground_truth")

        existing = client.get_evaluation(question_id) if client else None

        if existing is None:
            result = _compute_metrics(
                question_id=question_id,
                question_text=question_text,
                answer_text=answer_text,
                contexts=contexts,
                ground_truth=ground_truth,
            )
            results.append(result)
            if not dry_run and client:
                client.post_evaluation(result)
            else:
                typer.echo(f"[DRY-RUN] POST {result}")
        else:
            patch_payload = _compute_missing_metrics(
                existing=existing,
                question_id=question_id,
                question_text=question_text,
                answer_text=answer_text,
                contexts=contexts,
                ground_truth=ground_truth,
            )
            results.append(patch_payload)
            if not dry_run and client:
                client.patch_evaluation(question_id, patch_payload)
            else:
                typer.echo(f"[DRY-RUN] PATCH {question_id}: {patch_payload}")

    return results


def _compute_missing_metrics(
    existing: dict,
    question_id: str,
    question_text: str,
    answer_text: str,
    contexts: list[str],
    ground_truth: "str | None",
) -> dict:
    """existing 행에서 None인 필드만 계산하여 PATCH payload를 반환한다.
    컨텍스트가 없으면 RAGAS 지표는 계산 불가 → None 유지 (COALESCE가 기존 null 보존).
    """
    need_faithfulness = existing.get("faithfulness") is None
    need_answer_relevancy = existing.get("answerRelevancy") is None
    need_context_precision = existing.get("contextPrecision") is None
    need_context_recall = existing.get("contextRecall") is None
    need_citation_coverage = existing.get("citationCoverage") is None
    need_citation_correctness = existing.get("citationCorrectness") is None

    has_contexts = bool(contexts and contexts != [""])
    judge_model = os.getenv("RAGAS_OLLAMA_MODEL", "qwen2.5:7b")
    ollama_url = os.getenv("OLLAMA_URL", "http://jcg-office.tailedf4dc.ts.net:11434")

    patch: dict = {}

    try:
        llm = None
        emb = None

        if (need_faithfulness or need_answer_relevancy) and has_contexts:
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

            data = {
                "question": [question_text],
                "answer": [answer_text],
                "contexts": [contexts],
            }
            dataset = Dataset.from_dict(data)
            result = evaluate(dataset, metrics=[faithfulness, answer_relevancy])
            scores = result.to_pandas().iloc[0].to_dict()

            if need_faithfulness:
                patch["faithfulness"] = _safe(scores.get("faithfulness"))
            if need_answer_relevancy:
                patch["answerRelevancy"] = _safe(scores.get("answer_relevancy"))

        if (need_context_precision or need_context_recall) and has_contexts and ground_truth:
            from ragas.metrics import context_precision, context_recall
            from ragas import evaluate
            from ragas.llms import LangchainLLMWrapper
            from langchain_ollama import ChatOllama
            from datasets import Dataset

            if llm is None:
                llm = LangchainLLMWrapper(ChatOllama(base_url=ollama_url, model=judge_model))
            context_precision.llm = llm
            context_recall.llm = llm

            data_gt = {
                "question": [question_text],
                "answer": [answer_text],
                "contexts": [contexts],
                "ground_truth": [ground_truth],
            }
            ds_gt = Dataset.from_dict(data_gt)
            result_gt = evaluate(ds_gt, metrics=[context_precision, context_recall])
            gt_scores = result_gt.to_pandas().iloc[0].to_dict()

            if need_context_precision:
                patch["contextPrecision"] = _safe(gt_scores.get("context_precision"))
            if need_context_recall:
                patch["contextRecall"] = _safe(gt_scores.get("context_recall"))

        if (need_citation_coverage or need_citation_correctness) and has_contexts:
            from ragas.llms import LangchainLLMWrapper
            from langchain_ollama import ChatOllama

            if llm is None:
                llm = LangchainLLMWrapper(ChatOllama(base_url=ollama_url, model=judge_model))

            if need_citation_coverage:
                patch["citationCoverage"] = _compute_citation_coverage(llm, question_text, answer_text, contexts)
            if need_citation_correctness:
                patch["citationCorrectness"] = _compute_citation_correctness(llm, answer_text, contexts)

    except Exception as e:
        typer.echo(f"[eval-runner] missing metrics 계산 실패 ({question_id}): {e}", err=True)

    return patch


def _compute_metrics(
    question_id: str,
    question_text: str,
    answer_text: str,
    contexts: list[str] | None = None,
    ground_truth: str | None = None,
) -> dict:
    judge_model = os.getenv("RAGAS_OLLAMA_MODEL", "qwen2.5:7b")
    ollama_url = os.getenv("OLLAMA_URL", "http://jcg-office.tailedf4dc.ts.net:11434")
    effective_contexts = contexts if contexts else [""]

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

        metrics = [faithfulness, answer_relevancy]

        data: dict = {
            "question": [question_text],
            "answer": [answer_text],
            "contexts": [effective_contexts],
        }

        # context_precision·context_recall은 ground_truth가 있을 때만 계산
        ctx_precision_score: float | None = None
        ctx_recall_score: float | None = None
        if ground_truth:
            try:
                from ragas.metrics import context_precision, context_recall
                context_precision.llm = llm
                context_recall.llm = llm
                data_with_gt = {**data, "ground_truth": [ground_truth]}
                ds_gt = Dataset.from_dict(data_with_gt)
                result_gt = evaluate(ds_gt, metrics=[context_precision, context_recall])
                gt_scores = result_gt.to_pandas().iloc[0].to_dict()
                ctx_precision_score = _safe(gt_scores.get("context_precision"))
                ctx_recall_score = _safe(gt_scores.get("context_recall"))
            except Exception as e:
                typer.echo(f"[eval-runner] context 지표 계산 실패 ({question_id}): {e}", err=True)

        dataset = Dataset.from_dict(data)
        result = evaluate(dataset, metrics=metrics)
        scores = result.to_pandas().iloc[0].to_dict()

        citation_coverage = _compute_citation_coverage(llm, question_text, answer_text, effective_contexts)
        citation_correctness = _compute_citation_correctness(llm, answer_text, effective_contexts)

        return {
            "questionId": question_id,
            "faithfulness": _safe(scores.get("faithfulness")),
            "answerRelevancy": _safe(scores.get("answer_relevancy")),
            "contextPrecision": ctx_precision_score,
            "contextRecall": ctx_recall_score,
            "citationCoverage": citation_coverage,
            "citationCorrectness": citation_correctness,
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
            "citationCoverage": None,
            "citationCorrectness": None,
            "judgeProvider": "ollama",
            "judgeModel": judge_model,
        }


def _compute_citation_coverage(llm: object, question: str, answer: str, contexts: list[str]) -> "float | None":
    """각 context chunk가 answer에 실제로 활용됐는지 LLM이 판단. 사용된 청크 수 / 전체 청크 수."""
    if not contexts or contexts == [""]:
        return None
    used = 0
    for ctx in contexts:
        if not ctx.strip():
            continue
        prompt = (
            "다음 문서 조각이 답변 생성에 실제로 활용됐으면 YES, 그렇지 않으면 NO로만 답하세요.\n\n"
            f"질문: {question}\n답변: {answer}\n문서: {ctx}\n\n활용됨:"
        )
        try:
            resp = llm.invoke(prompt).content.strip().upper()
            if resp.startswith("YES"):
                used += 1
        except Exception:
            pass
    valid = len([c for c in contexts if c.strip()])
    return used / valid if valid > 0 else None


def _compute_citation_correctness(llm: object, answer: str, contexts: list[str]) -> "float | None":
    """각 context chunk가 answer 내용을 올바르게 지지하는지 판단. 지지 청크 수 / 전체 청크 수."""
    if not contexts or contexts == [""]:
        return None
    correct = 0
    for ctx in contexts:
        if not ctx.strip():
            continue
        prompt = (
            "다음 문서 조각이 답변의 내용을 올바르게 지지하면 YES, 왜곡·불일치하면 NO로만 답하세요.\n\n"
            f"답변: {answer}\n문서: {ctx}\n\n지지함:"
        )
        try:
            resp = llm.invoke(prompt).content.strip().upper()
            if resp.startswith("YES"):
                correct += 1
        except Exception:
            pass
    valid = len([c for c in contexts if c.strip()])
    return correct / valid if valid > 0 else None


def _safe(v: object) -> "float | None":
    try:
        f = float(v)  # type: ignore[arg-type]
        return None if math.isnan(f) else f
    except (TypeError, ValueError):
        return None


def main() -> None:
    from dotenv import load_dotenv
    load_dotenv()
    app()


if __name__ == "__main__":
    main()
