from __future__ import annotations

import hashlib
import json
import os
import re
import time
from typing import List, Optional

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="rag-orchestrator")


class ConversationMessage(BaseModel):
    role: str
    content: str


class GenerateAnswerRequest(BaseModel):
    question_id: str
    question_text: str
    organization_id: str
    service_id: str
    conversation_history: List[ConversationMessage] = []


class GenerateAnswerResponse(BaseModel):
    question_id: str
    answer_text: str
    answer_status: str
    citation_count: int
    response_time_ms: int
    fallback_reason_code: Optional[str] = None
    confidence_score: Optional[float] = None
    question_failure_reason_code: Optional[str] = None
    is_escalated: bool = False
    query_embedding: Optional[List[float]] = None
    model_name: Optional[str] = None
    provider_name: str = "ollama"
    input_tokens: Optional[int] = None
    output_tokens: Optional[int] = None
    total_tokens: Optional[int] = None


def _cache_key(question_text: str, org_id: str) -> str:
    normalized = " ".join(question_text.strip().lower().split())
    return f"rag:cache:{org_id}:{hashlib.sha256(normalized.encode()).hexdigest()}"


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
    admin_api_url = os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")
    use_ollama = check_ollama_available(ollama_url)

    from .config_client import fetch_rag_config
    rag_config = fetch_rag_config(request.organization_id, admin_api_url)

    # Redis 캐시 조회
    redis_url = os.getenv("REDIS_URL", "redis://localhost:6379")
    cache_ttl = int(os.getenv("RAG_CACHE_TTL_SEC", "86400"))
    _redis_client = None
    _cached_answer = None
    try:
        import redis as redis_lib
        _redis_client = redis_lib.from_url(redis_url, decode_responses=True, socket_connect_timeout=1)
        _cache_key_str = _cache_key(request.question_text, request.organization_id)
        _cached_raw = _redis_client.get(_cache_key_str)
        if _cached_raw:
            _cached_answer = json.loads(_cached_raw)
    except Exception:
        pass

    if _cached_answer:
        _log_search_result(
            question_id=request.question_id,
            query_text=request.question_text,
            search_results=[],
            latency_ms=0,
            cache_hit=True,
        )
        return GenerateAnswerResponse(**_cached_answer)

    if use_ollama:
        start_ms = int(time.time() * 1000)

        from .retrieval import get_embedding, hybrid_search
        top_k = rag_config.get("topK", int(os.getenv("HYBRID_SEARCH_TOP_K", "10")))
        reranker_enabled = rag_config.get("rerankerEnabled", os.getenv("RERANKER_ENABLED", "false").lower() == "true")

        retrieval_start = int(time.time() * 1000)
        search_results = hybrid_search(
            query_text=request.question_text,
            top_k=top_k,
            ollama_url=ollama_url,
            db_url=db_url,
            reranker_enabled=reranker_enabled,
        )
        query_embedding = get_embedding(request.question_text, ollama_url)
        latency_ms = int(time.time() * 1000) - retrieval_start

        llm_start = int(time.time() * 1000)
        llm_result = generate_answer_with_ollama(
            question=request.question_text,
            search_results=search_results,
            ollama_url=ollama_url,
            rag_config=rag_config,
        )
        llm_ms = int(time.time() * 1000) - llm_start

        postprocess_start = int(time.time() * 1000)
        answer_text = llm_result["content"]
        model_name = llm_result.get("model")
        input_tokens = llm_result.get("input_tokens")
        output_tokens = llm_result.get("output_tokens")
        total_tokens = (input_tokens or 0) + (output_tokens or 0) or None

        answer_status = "answered"
        citation_count = len(search_results)
        fallback_reason = None
        response_time_ms = int(time.time() * 1000) - start_ms

        if search_results:
            distances = [float(r["distance"]) for r in search_results]
            avg_distance = sum(distances) / len(distances)
            confidence_score = round(max(0.0, 1.0 - avg_distance), 4)
            if confidence_score < 0.4:
                question_failure_reason_code = "A05"
                is_escalated = True
            else:
                question_failure_reason_code = None
                is_escalated = False
        else:
            confidence_score = 0.0
            question_failure_reason_code = "A04"
            is_escalated = True
        postprocess_ms = int(time.time() * 1000) - postprocess_start

        # 검색 로그 Admin API 콜백
        _log_search_result(
            question_id=request.question_id,
            query_text=request.question_text,
            search_results=search_results,
            latency_ms=latency_ms,
            llm_ms=llm_ms,
            postprocess_ms=postprocess_ms,
            cache_hit=False,
        )
    else:
        answer_text = f"[DEV MODE] Ollama not available. Stub answer for: {request.question_text}"
        answer_status = "fallback"
        citation_count = 0
        fallback_reason = "OLLAMA_NOT_AVAILABLE"
        response_time_ms = 0
        confidence_score = None
        question_failure_reason_code = "A04"
        is_escalated = True
        query_embedding = None
        model_name = None
        input_tokens = None
        output_tokens = None
        total_tokens = None

    # 캐시 저장 (MISS 경로)
    if _redis_client:
        try:
            _resp_dict = {
                "question_id": request.question_id,
                "answer_text": answer_text,
                "answer_status": answer_status,
                "citation_count": citation_count,
                "response_time_ms": response_time_ms,
                "fallback_reason_code": fallback_reason,
                "confidence_score": confidence_score,
                "question_failure_reason_code": question_failure_reason_code,
                "is_escalated": is_escalated,
                "query_embedding": None,
                "model_name": model_name,
                "provider_name": "ollama",
                "input_tokens": input_tokens,
                "output_tokens": output_tokens,
                "total_tokens": total_tokens,
            }
            _redis_client.setex(_cache_key_str, cache_ttl, json.dumps(_resp_dict))
        except Exception:
            pass

    return GenerateAnswerResponse(
        question_id=request.question_id,
        answer_text=answer_text,
        answer_status=answer_status,
        citation_count=citation_count,
        response_time_ms=response_time_ms,
        fallback_reason_code=fallback_reason,
        confidence_score=confidence_score,
        question_failure_reason_code=question_failure_reason_code,
        is_escalated=is_escalated,
        query_embedding=query_embedding,
        model_name=model_name,
        provider_name="ollama",
        input_tokens=input_tokens,
        output_tokens=output_tokens,
        total_tokens=total_tokens,
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
    rag_config: dict | None = None,
) -> dict:
    """
    Ollama API를 사용해 검색된 컨텍스트 기반 답변을 생성한다.

    반환값: {"content": str, "model": str, "input_tokens": int|None, "output_tokens": int|None}
    Ollama stream=false 응답에서 prompt_eval_count(입력 토큰), eval_count(출력 토큰)를 추출한다.
    """
    import httpx

    if search_results:
        context_chunks = "\n\n".join([
            f"[문서 {i+1}] {chunk['chunk_text']}"
            for i, chunk in enumerate(search_results)
        ])
        retrieval_context = f"검색된 관련 문서:\n\n{context_chunks}"
    else:
        retrieval_context = "관련 문서를 찾지 못했습니다. 일반적인 정보로 답변드립니다."

    cfg = rag_config or {}
    system_prompt = cfg.get("systemPrompt") or (
        "당신은 공공기관 민원 안내 챗봇입니다. "
        "제공된 문서를 기반으로 정확하고 친절하게 답변하세요. "
        "답변을 찾을 수 없으면 모른다고 말하세요."
    )
    user_prompt = f"""참고 문서:
{retrieval_context}

질문: {question}

위 문서를 참고하여 질문에 답변해주세요."""

    model = cfg.get("llmModel") or os.getenv("OLLAMA_MODEL", "qwen2.5:7b")
    temperature = float(cfg.get("llmTemperature", 0.3))
    num_predict = int(cfg.get("llmMaxTokens", 500))

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
                "options": {"temperature": temperature, "num_predict": num_predict},
            },
            timeout=30.0,
        )

        if response.status_code == 200:
            result = response.json()
            raw_content = result.get("message", {}).get("content", "[답변 생성 실패]")
            # 유효하지 않은 JSON escape 시퀀스 제거 (예: \! → !)
            # Jackson이 \! 를 포함한 JSON을 파싱할 때 JsonParseException 발생 방지
            content = re.sub(r'\\([^"\\/bfnrtu\n\r\t])', r'\1', raw_content)
            return {
                "content": content,
                "model": result.get("model", model),
                "input_tokens": result.get("prompt_eval_count"),
                "output_tokens": result.get("eval_count"),
            }
        else:
            return {"content": f"[Ollama API 오류: {response.status_code}]", "model": model,
                    "input_tokens": None, "output_tokens": None}

    except Exception as e:
        return {"content": f"[Ollama 호출 실패: {str(e)}]", "model": model,
                "input_tokens": None, "output_tokens": None}


@app.post("/generate/stream")
def generate_answer_stream(request: GenerateAnswerRequest):
    """
    질문에 대한 답변을 NDJSON 스트리밍으로 반환한다.

    retrieval은 동기로 수행하고, LLM 생성만 stream=True로 토큰 단위로 emit한다.
    청크 형식:
      {"content": "토큰"}        -- LLM 생성 중
      {"done": true, ...}        -- 완료 신호 (마지막)
    """
    from fastapi.responses import StreamingResponse
    import json

    ollama_url = os.getenv("OLLAMA_URL", "http://localhost:11434")
    db_url = os.getenv("DATABASE_URL")
    admin_api_url = os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")
    use_ollama = check_ollama_available(ollama_url)

    from .config_client import fetch_rag_config
    rag_config = fetch_rag_config(request.organization_id, admin_api_url)

    if not use_ollama:
        def fallback_stream():
            stub = f"[DEV MODE] Ollama not available. Stub answer for: {request.question_text}"
            yield json.dumps({"content": stub}) + "\n"
            yield json.dumps({
                "done": True,
                "answer_status": "fallback",
                "citation_count": 0,
                "response_time_ms": 0,
                "confidence_score": 0.0,
                "retrieved_chunks": [],
            }) + "\n"
        return StreamingResponse(fallback_stream(), media_type="application/x-ndjson")

    from .retrieval import get_embedding, hybrid_search
    start_ms = int(time.time() * 1000)
    top_k = rag_config.get("topK", int(os.getenv("HYBRID_SEARCH_TOP_K", "10")))
    reranker_enabled = rag_config.get("rerankerEnabled", os.getenv("RERANKER_ENABLED", "false").lower() == "true")

    retrieval_start = int(time.time() * 1000)
    search_results = hybrid_search(
        query_text=request.question_text,
        top_k=top_k,
        ollama_url=ollama_url,
        db_url=db_url,
        reranker_enabled=reranker_enabled,
    )
    get_embedding(request.question_text, ollama_url)
    latency_ms = int(time.time() * 1000) - retrieval_start

    _log_search_result(
        question_id=request.question_id,
        query_text=request.question_text,
        search_results=search_results,
        latency_ms=latency_ms,
        llm_ms=None,  # streaming: LLM 소요시간은 done 이벤트 후 알 수 있으므로 별도 추적 불가
        cache_hit=False,
    )

    citation_count = len(search_results)
    if search_results:
        distances = [float(r["distance"]) for r in search_results]
        avg_distance = sum(distances) / len(distances)
        confidence_score = round(max(0.0, 1.0 - avg_distance), 4)
    else:
        confidence_score = 0.0

    if search_results:
        context_chunks = "\n\n".join([
            f"[문서 {i+1}] {chunk['chunk_text']}"
            for i, chunk in enumerate(search_results)
        ])
        retrieval_context = f"검색된 관련 문서:\n\n{context_chunks}"
    else:
        retrieval_context = "관련 문서를 찾지 못했습니다. 일반적인 정보로 답변드립니다."

    system_prompt = rag_config.get("systemPrompt") or (
        "당신은 공공기관 민원 안내 챗봇입니다. "
        "제공된 문서를 기반으로 정확하고 친절하게 답변하세요. "
        "답변을 찾을 수 없으면 모른다고 말하세요."
    )
    user_prompt = f"""참고 문서:
{retrieval_context}

질문: {request.question_text}

위 문서를 참고하여 질문에 답변해주세요."""
    model = rag_config.get("llmModel") or os.getenv("OLLAMA_MODEL", "qwen2.5:7b")
    temperature = float(rag_config.get("llmTemperature", 0.3))
    num_predict = int(rag_config.get("llmMaxTokens", 500))

    # 시스템 프롬프트 + 이전 대화 히스토리 + 현재 질문(+컨텍스트)
    ollama_messages = [{"role": "system", "content": system_prompt}]
    for h in request.conversation_history:
        ollama_messages.append({"role": h.role, "content": h.content})
    ollama_messages.append({"role": "user", "content": user_prompt})

    def token_stream():
        import httpx
        with httpx.stream(
            "POST",
            f"{ollama_url}/api/chat",
            json={
                "model": model,
                "messages": ollama_messages,
                "stream": True,
                "options": {"temperature": temperature, "num_predict": num_predict},
            },
            timeout=60.0,
        ) as r:
            for line in r.iter_lines():
                if not line:
                    continue
                try:
                    chunk = json.loads(line)
                    token = chunk.get("message", {}).get("content", "")
                    if token:
                        yield json.dumps({"content": token}) + "\n"
                except Exception:
                    continue
        response_time_ms = int(time.time() * 1000) - start_ms
        yield json.dumps({
            "done": True,
            "answer_status": "answered",
            "citation_count": citation_count,
            "response_time_ms": response_time_ms,
            "confidence_score": confidence_score,
            "retrieved_chunks": [
                {
                    "filename": r.get("filename", "unknown"),
                    "preview": r["chunk_text"][:200],
                    "score": round(1.0 - float(r["distance"]), 3),
                }
                for r in search_results
            ],
        }) + "\n"

    return StreamingResponse(token_stream(), media_type="application/x-ndjson")


def _log_search_result(
    question_id: str,
    query_text: str,
    search_results: list[dict[str, str]],
    latency_ms: int,
    llm_ms: int | None = None,
    postprocess_ms: int | None = None,
    cache_hit: bool = False,
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
                "llmMs": llm_ms,
                "postprocessMs": postprocess_ms,
                "retrievalEngine": "pgvector",
                "retrievalStatus": retrieval_status,
                "cacheHit": cache_hit,
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
