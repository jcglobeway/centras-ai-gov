# Proposal

## Change ID

`add-chat-runtime-module`

## Summary

### 변경 목적
- Chat Runtime 모듈 구현 (questions, answers, chat_sessions)
- QA Review ↔ Questions FK 연결
- Unresolved queue 구현 (MVP 핵심 기능!)
- 전체 MVP 루프 완성

### 변경 범위
- Flyway migration 4개:
  - V009: chat_sessions
  - V010: questions
  - V011: answers
  - V012: qa_reviews FK 추가 (ALTER TABLE)
- chat-runtime JPA 엔티티 3개
- chat-runtime Repository + 어댑터
- API: questions, answers, unresolved queue
- 테스트

### 제외 범위
- rag_search_logs 테이블 (별도 change)
- 실제 RAG 엔진 연동
- 시민용 챗봇 UI

## Impact

- modules/chat-runtime: JPA 구현
- apps/admin-api: Chat API 추가
- modules/qa-review: questions FK 연결

## Done Definition

- [ ] Flyway migration 4개
- [ ] JPA 엔티티 3개
- [ ] Repository 어댑터 6개
- [ ] API 추가
- [ ] Unresolved queue 구현
- [ ] 테스트 추가
- [ ] ./gradlew test 통과
