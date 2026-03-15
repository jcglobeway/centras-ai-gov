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
    # 개발 환경: 간단한 stub 응답
    use_openai = os.getenv("OPENAI_API_KEY") is not None

    if use_openai:
        # OpenAI API를 사용한 간단한 답변 생성 (실제 retrieval 없이)
        answer_text = generate_answer_with_llm(request.question_text)
        answer_status = "answered"
        citation_count = 0  # stub: 실제 retrieval 없음
        fallback_reason = None
    else:
        # LLM API 없으면 fallback
        answer_text = f"[DEV MODE] This is a stub answer for: {request.question_text}"
        answer_status = "fallback"
        citation_count = 0
        fallback_reason = "DEV_MODE_STUB"

    return GenerateAnswerResponse(
        question_id=request.question_id,
        answer_text=answer_text,
        answer_status=answer_status,
        citation_count=citation_count,
        response_time_ms=500,  # stub
        fallback_reason_code=fallback_reason,
    )


def generate_answer_with_llm(question: str) -> str:
    """LLM API를 사용해 답변을 생성한다 (stub)."""
    # 현재는 간단한 stub 응답
    # 실제 구현 시: OpenAI API 호출
    return f"AI generated answer for: {question}"


def main() -> None:
    import uvicorn

    uvicorn.run("rag_orchestrator.app:app", host="0.0.0.0", port=8090, reload=False)


if __name__ == "__main__":
    main()

