from __future__ import annotations

import os
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
    질문에 대한 답변을 생성한다 (현재는 stub 구현).

    실제 구현 시:
    1. Query rewrite
    2. Vector retrieval (OpenSearch/pgvector)
    3. Reranking
    4. LLM answer synthesis
    5. Citation extraction
    """
    # Ollama를 사용한 답변 생성
    ollama_url = os.getenv("OLLAMA_URL", "http://localhost:11434")
    use_ollama = check_ollama_available(ollama_url)

    if use_ollama:
        # Ollama API를 사용한 답변 생성 (실제 retrieval 없이)
        answer_text = generate_answer_with_ollama(
            question=request.question_text,
            ollama_url=ollama_url,
        )
        answer_status = "answered"
        citation_count = 0  # stub: 실제 retrieval 없음
        fallback_reason = None
    else:
        # Ollama 없으면 fallback
        answer_text = f"[DEV MODE] Ollama not available. Stub answer for: {request.question_text}"
        answer_status = "fallback"
        citation_count = 0
        fallback_reason = "OLLAMA_NOT_AVAILABLE"

    return GenerateAnswerResponse(
        question_id=request.question_id,
        answer_text=answer_text,
        answer_status=answer_status,
        citation_count=citation_count,
        response_time_ms=500,  # stub
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


def generate_answer_with_ollama(question: str, ollama_url: str) -> str:
    """Ollama API를 사용해 답변을 생성한다."""
    import httpx

    # Mock retrieval context (향후 실제 vector search로 교체)
    mock_context = """
    서울시 복지 혜택 신청 안내:
    - 신청 방법: 온라인 또는 주민센터 방문
    - 필요 서류: 신분증, 소득 증빙 서류
    - 처리 기간: 신청 후 7-14일
    """

    system_prompt = "당신은 공공기관 민원 안내 챗봇입니다. 제공된 문서를 기반으로 정확하고 친절하게 답변하세요."
    user_prompt = f"""참고 문서:
{mock_context}

질문: {question}

위 문서를 참고하여 질문에 답변해주세요."""

    # Ollama API 호출 (chat completion)
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


def main() -> None:
    import uvicorn

    uvicorn.run("rag_orchestrator.app:app", host="0.0.0.0", port=8090, reload=False)


if __name__ == "__main__":
    main()

