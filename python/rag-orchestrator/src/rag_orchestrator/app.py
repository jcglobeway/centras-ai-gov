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
    """LLM API를 사용해 답변을 생성한다."""
    from openai import OpenAI

    client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

    # Mock retrieval context (향후 실제 vector search로 교체)
    mock_context = """
    서울시 복지 혜택 신청 안내:
    - 신청 방법: 온라인 또는 주민센터 방문
    - 필요 서류: 신분증, 소득 증빙 서류
    - 처리 기간: 신청 후 7-14일
    """

    # LLM 호출
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {
                "role": "system",
                "content": "당신은 공공기관 민원 안내 챗봇입니다. 제공된 문서를 기반으로 정확하고 친절하게 답변하세요.",
            },
            {
                "role": "user",
                "content": f"""참고 문서:
{mock_context}

질문: {question}

위 문서를 참고하여 질문에 답변해주세요.""",
            },
        ],
        temperature=0.3,
        max_tokens=500,
    )

    return response.choices[0].message.content or "[답변 생성 실패]"


def main() -> None:
    import uvicorn

    uvicorn.run("rag_orchestrator.app:app", host="0.0.0.0", port=8090, reload=False)


if __name__ == "__main__":
    main()

