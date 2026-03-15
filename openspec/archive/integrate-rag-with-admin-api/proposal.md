# Proposal

## Change ID

`integrate-rag-with-admin-api`

## Summary

### 변경 목적
- admin-api가 rag-orchestrator를 호출하여 자동으로 답변 생성
- 질문 생성 → 답변 생성 → 저장의 전체 플로우 자동화
- MVP 실제 작동 검증

### 변경 범위
- QuestionController에 rag-orchestrator 호출 로직 추가
- RagOrchestratorClient (httpx 기반)
- 답변 자동 저장
- 환경 변수: RAG_ORCHESTRATOR_URL

### 제외 범위
- Vector search
- Reranking
- 복잡한 에러 재시도 로직

## Done Definition

- [ ] RagOrchestratorClient 구현
- [ ] QuestionController에 자동 답변 생성 로직 추가
- [ ] 테스트 추가
- [ ] ./gradlew test 통과
