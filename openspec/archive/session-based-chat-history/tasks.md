# Tasks

## 백엔드

### chat-runtime 모듈

- [x] B1. `JpaChatSessionRepository` — `findByOrganizationIdInOrderByStartedAtDesc` / `findAllByOrderByStartedAtDesc` 추가
- [x] B2. `JpaQuestionRepository` — `findAllWithAnswersBySessionId` 네이티브 쿼리 추가
- [x] B3. `LoadChatSessionPort` 인터페이스 신규 작성
- [x] B4. `LoadChatSessionPortAdapter` 구현체 신규 작성
- [x] B5. `ListChatSessionsUseCase` 인터페이스 신규 작성
- [x] B6. `ListChatSessionsService` 서비스 신규 작성
- [x] B7. `LoadQuestionPort` — `listQuestionsWithAnswersBySession` 메서드 추가
- [x] B8. `LoadQuestionPortAdapter` — B7 구현
- [x] B9. `ListQuestionsUseCase` — `listBySession` 메서드 추가
- [x] B10. `ListQuestionsService` — B9 구현

### admin-api

- [x] B11. `ChatSessionController` 신규 작성 (3개 엔드포인트: list, get, questions)
- [x] B12. `RepositoryConfiguration` — `loadChatSessionPort` Bean 등록
- [x] B13. `ServiceConfiguration` — `listChatSessionsUseCase` Bean 등록

## 프론트엔드

- [x] F1. `types.ts` — `ChatSession` 인터페이스 추가
- [x] F2. `/ops/chat-history/page.tsx` — 세션 목록 테이블로 변환
- [x] F3. `/ops/chat-history/[sessionId]/page.tsx` — 세션 상세 페이지 신규 작성 (DetailPanel 포함)

## 검증

- [ ] V1. `npm run dev` → `/ops/chat-history` 세션 목록 표시 확인
- [ ] V2. 세션 행 클릭 → `/ops/chat-history/[sessionId]` 이동 확인
- [ ] V3. 세션 상세 — 질문/답변 대화 스레드 표시 확인
- [ ] V4. 질문 클릭 → DetailPanel 슬라이드 표시 확인
- [ ] V5. breadcrumb "← 대화 이력" 클릭 → 세션 목록 복귀 확인
- [ ] V6. 기존 50개 통합 테스트 통과 확인
