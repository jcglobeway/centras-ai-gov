# Proposal

## Change ID

`add-rag-orchestrator-service`

## Summary

### 변경 목적
- RAG Orchestrator Python 서비스 구현 (FastAPI)
- 질문에 대한 자동 답변 생성 구현
- admin-api와 연동하여 MVP 전체 플로우 완성

### 변경 범위
- python/rag-orchestrator FastAPI app
- Query rewrite stub
- Retrieval adapter stub
- Answer synthesis (LLM API 호출)
- admin-api 연동

### 제외 범위
- 실제 OpenSearch/pgvector 연동 (stub으로 시작)
- Reranking 로직
- Advanced query expansion

## Done Definition

- [ ] FastAPI app 구조
- [ ] POST /generate endpoint
- [ ] LLM API 호출 (OpenAI/Claude stub)
- [ ] pyproject.toml 의존성
- [ ] ./gradlew test 통과
