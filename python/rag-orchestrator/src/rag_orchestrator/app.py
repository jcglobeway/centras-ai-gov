from __future__ import annotations

import os
import time
from typing import Optional

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="rag-orchestrator")


class GenerateAnswerRequest(BaseModel):
    question_id: str
    question_text: str
    organization_id: str
    service_id: str


class GenerateAnswerResponse(BaseModel):
    question_id: str
    answer_text: str
    answer_status: str
    citation_count: int
    response_time_ms: int
    fallback_reason_code: Optional[str] = None


@app.get("/healthz")
def healthz() -> dict[str, str]:
    return {"status": "ok", "service": "rag-orchestrator"}


@app.post("/generate")
def generate_answer(request: GenerateAnswerRequest) -> GenerateAnswerResponse:
    """
    질문에 대한 답변을 생성한다.

    1. Ollama bge-m3로 query embedding 생성
    2. pgvector cosine similarity search로 관련 청크 검색
    3. 검색 결과를 Admin API에 rag_search_log로 기록
    4. Ollama LLM으로 답변 합성
    """
    ollama_url = os.getenv("OLLAMA_URL", "http://localhost:11434")
    db_url = os.getenv("DATABASE_URL")
    use_ollama = check_ollama_available(ollama_url)

    if use_ollama:
        start_ms = int(time.time() * 1000)

        from .retrieval import vector_search
        search_results = vector_search(
            query_text=request.question_text,
            top_k=3,
            ollama_url=ollama_url,
            db_connection_string=db_url,
        )

        latency_ms = int(time.time() * 1000) - start_ms

        # 검색 로그 Admin API 콜백
        _log_search_result(
            question_id=request.question_id,
            query_text=request.question_text,
            search_results=search_results,
            latency_ms=latency_ms,
        )

        answer_text = generate_answer_with_ollama(
            question=request.question_text,
            search_results=search_results,
            ollama_url=ollama_url,
        )
        answer_status = "answered"
        citation_count = len(search_results)
        fallback_reason = None
        response_time_ms = int(time.time() * 1000) - start_ms
    else:
        answer_text = f"[DEV MODE] Ollama not available. Stub answer for: {request.question_text}"
        answer_status = "fallback"
        citation_count = 0
        fallback_reason = "OLLAMA_NOT_AVAILABLE"
        response_time_ms = 0

    return GenerateAnswerResponse(
        question_id=request.question_id,
        answer_text=answer_text,
        answer_status=answer_status,
        citation_count=citation_count,
        response_time_ms=response_time_ms,
        fallback_reason_code=fallback_reason,
    )


def check_ollama_available(ollama_url: str) -> bool:
    """Ollama가 실행 중인지 확인한다."""
    import httpx

    try:
        response = httpx.get(f"{ollama_url}/api/tags", timeout=2.0)
        return response.status_code == 200
    except Exception:
        return False


def generate_answer_with_ollama(
    question: str,
    search_results: list[dict[str, str]],
    ollama_url: str,
) -> str:
    """Ollama API를 사용해 검색된 컨텍스트 기반 답변을 생성한다."""
    import httpx

    if search_results:
        context_chunks = "\n\n".join([
            f"[문서 {i+1}] {chunk['chunk_text']}"
            for i, chunk in enumerate(search_results)
        ])
        retrieval_context = f"검색된 관련 문서:\n\n{context_chunks}"
    else:
        # pgvector 검색 결과가 없을 때 기본 안내 제공
        retrieval_context = "관련 문서를 찾지 못했습니다. 일반적인 정보로 답변드립니다."

    system_prompt = "당신은 공공기관 민원 안내 챗봇입니다. 제공된 문서를 기반으로 정확하고 친절하게 답변하세요."
    user_prompt = f"""참고 문서:
{retrieval_context}

질문: {question}

위 문서를 참고하여 질문에 답변해주세요."""

    model = os.getenv("OLLAMA_MODEL", "qwen2.5:7b")

    try:
        response = httpx.post(
            f"{ollama_url}/api/chat",
            json={
                "model": model,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                "stream": False,
                "options": {
                    "temperature": 0.3,
                    "num_predict": 500,
                },
            },
            timeout=30.0,
        )

        if response.status_code == 200:
            result = response.json()
            return result.get("message", {}).get("content", "[답변 생성 실패]")
        else:
            return f"[Ollama API 오류: {response.status_code}]"

    except Exception as e:
        return f"[Ollama 호출 실패: {str(e)}]"


def _log_search_result(
    question_id: str,
    query_text: str,
    search_results: list[dict[str, str]],
    latency_ms: int,
) -> None:
    """
    RAG 검색 결과를 Admin API rag-search-log 엔드포인트로 기록한다.

    Admin API가 없거나 실패해도 RAG 답변 생성 흐름에는 영향을 주지 않는다.
    """
    import httpx

    admin_api_url = os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")
    retrieval_status = "success" if search_results else "zero_result"

    try:
        httpx.post(
            f"{admin_api_url}/admin/rag-search-logs",
            json={
                "questionId": question_id,
                "queryText": query_text,
                "topK": len(search_results),
                "latencyMs": latency_ms,
                "retrievalEngine": "pgvector",
                "retrievalStatus": retrieval_status,
                "retrievedChunks": [
                    {
                        "chunkId": r["chunk_id"],
                        "rank": i + 1,
                        "score": float(r["distance"]),
                        "usedInCitation": True,
                    }
                    for i, r in enumerate(search_results)
                ],
            },
            timeout=3.0,
        )
    except Exception:
        # Admin API 콜백 실패는 무시 (best-effort logging)
        pass


# ── RAGAS 평가 엔드포인트 ─────────────────────────────────────────────────────

class EvaluationSample(BaseModel):
    question_id: str
    question_text: str
    answer_text: str
    contexts: list[str] = []
    ground_truth: Optional[str] = None


class EvaluationRequest(BaseModel):
    samples: list[EvaluationSample]
    judge_provider: str = "ollama"
    judge_model: str = "qwen2.5:7b"


class EvaluationMetrics(BaseModel):
    question_id: str
    faithfulness: Optional[float] = None
    answer_relevancy: Optional[float] = None
    context_precision: Optional[float] = None
    context_recall: Optional[float] = None


class EvaluationResponse(BaseModel):
    results: list[EvaluationMetrics]
    evaluated_count: int


@app.post("/evaluate")
def evaluate_ragas(request: EvaluationRequest) -> EvaluationResponse:
    """
    RAGAS 지표를 계산해 반환한다.

    ragas 패키지가 없거나 judge LLM이 응답하지 않으면 None 값을 포함한 결과를 반환한다.
    """
    results = []
    for sample in request.samples:
        metrics = _compute_ragas_metrics(sample=sample)
        results.append(metrics)
    return EvaluationResponse(results=results, evaluated_count=len(results))


def _compute_ragas_metrics(sample: EvaluationSample) -> EvaluationMetrics:
    try:
        from ragas.metrics import faithfulness, answer_relevancy
        from ragas import evaluate
        from datasets import Dataset

        data: dict = {
            "question": [sample.question_text],
            "answer": [sample.answer_text],
            "contexts": [sample.contexts if sample.contexts else [""]],
        }
        if sample.ground_truth:
            data["ground_truth"] = [sample.ground_truth]

        dataset = Dataset.from_dict(data)
        result = evaluate(dataset, metrics=[faithfulness, answer_relevancy])
        scores = result.to_pandas().iloc[0].to_dict()

        return EvaluationMetrics(
            question_id=sample.question_id,
            faithfulness=scores.get("faithfulness"),
            answer_relevancy=scores.get("answer_relevancy"),
        )
    except Exception:
        return EvaluationMetrics(question_id=sample.question_id)


def main() -> None:
    import uvicorn
    from dotenv import load_dotenv

    load_dotenv()
    uvicorn.run("rag_orchestrator.app:app", host="0.0.0.0", port=8090, reload=False)


if __name__ == "__main__":
    main()
