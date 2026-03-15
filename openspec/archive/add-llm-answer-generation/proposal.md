# Proposal

## Change ID

`add-llm-answer-generation`

## Summary

### 변경 목적
- RAG orchestrator에 실제 LLM API 호출 구현
- Mock retrieval + 실제 답변 생성으로 빠르게 작동 확인
- 향후 실제 vector search로 교체 가능한 구조

### 변경 범위
- OpenAI API 실제 호출 구현
- Mock retrieval context (하드코딩 문서)
- Citation 생성
- 환경 변수 설정

### 제외 범위
- 실제 vector search
- Embedding 생성
- Reranking

## Done Definition

- [ ] OpenAI API 호출 구현
- [ ] Mock retrieval context
- [ ] Citation 생성
- [ ] 환경 변수 처리
- [ ] 테스트
