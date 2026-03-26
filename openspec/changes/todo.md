# Todo

미결 OpenSpec 변경사항 통합 목록.
각 항목은 `openspec/changes/<change-id>/` 의 상세 문서를 참조한다.

---

## 1. question-embedding — FAQ 후보 E2E 검증

**Change**: `openspec/archive/question-embedding/`
**상태**: 구현 완료, PostgreSQL 환경에서 E2E 검증 미완

### 미결 항목

- [ ] `GET /admin/faq-candidates?organization_id=org_acc&threshold=0.85` 응답 확인
  - 전제조건: query-runner로 충분한 질문 임베딩이 쌓인 후 실행
  - 검증: 유사 질문 쌍이 `items` 배열에 반환되는지 확인
  ```bash
  curl "http://localhost:8080/admin/faq-candidates?organization_id=org_acc&threshold=0.85" \
    -H "X-Admin-Session-Id: <session>"
  ```

---

## 2. dashboard-sync — 대시보드 PRD 정렬

**Change**: `openspec/changes/dashboard-sync/`
**상태**: `in_progress` — Phase 1~7 전부 미구현

### Phase 1 — QuestionResponse 확장 (백엔드)

- [ ] `QuestionResponse`에 `failureReasonCode`, `questionCategory`, `isEscalated`, `answerConfidence` 추가
- [ ] `toResponse()` 매핑 4개 필드 추가
- [ ] 테스트: GET /admin/questions 응답 검증

### Phase 2 — 미결질문 answerStatus + latestReviewStatus (백엔드)

- [ ] `UnresolvedRow` projection + `findUnresolvedWithStatus()` native query 추가
- [ ] `UnresolvedQuestionSummary` 도메인 모델 추가
- [ ] `LoadQuestionPort` / `ListQuestionsUseCase` / `ListQuestionsService` / `LoadQuestionPortAdapter` 반환 타입 변경
- [ ] `UnresolvedQuestionResponse` DTO 신규 + `QuestionController.listUnresolvedQuestions()` 교체
- [ ] 테스트 50개 통과 확인

### Phase 3 — 프론트엔드 타입 동기화

- [ ] `types.ts` `Question` 인터페이스 필드명 백엔드 일치
  - `sessionId` → `chatSessionId`
  - `failureCode` → `failureReasonCode`
  - `wasTransferred` → `isEscalated`
  - `categoryL1/L2` → `questionCategory`
- [ ] `types.ts` `UnresolvedQuestion` 인터페이스 추가 (`answerStatus`, `latestReviewStatus` 포함)
- [ ] `types.ts` `DailyMetric` V023 7개 필드 추가

### Phase 4 — 프론트엔드 페이지 필드명 수정

- [ ] `client/failure/page.tsx`: `q.failureCode` → `q.failureReasonCode`
- [ ] `qa/page.tsx`: `q.failureCode` → `q.failureReasonCode`
- [ ] `qa/unresolved/page.tsx`: `UnresolvedQuestion` 타입 사용
- [ ] `ops/page.tsx`: answerStatus 없는 Question 타입 처리 수정

### Phase 5 — QA Review review_status 필터 + confirmedCount 수정

- [ ] `JpaQAReviewRepository.findByReviewStatus()` 추가
- [ ] `LoadQAReviewPort.listByStatus()` 추가 (UseCase/Service/Adapter 포함)
- [ ] `QAReviewController.listQAReviews()` `review_status` 파라미터 추가
- [ ] `qa/page.tsx` confirmedCount를 `total` 기반으로 수정

### Phase 6 — V023 온디맨드 집계

- [ ] `DailyMetricsSummary` V023 7개 필드 추가
- [ ] `LoadDailyMetricsPortAdapter.aggregateFromRawData()` native SQL 집계 구현
- [ ] `DailyMetricsResponse` V023 필드 추가 + `toResponse()` 매핑
- [ ] `client/page.tsx` V023 필드 사용 (autoResolutionRate, escalationRate 등)

### Phase 7 — client/performance/page.tsx 교체

- [ ] `/admin/questions` API 사용으로 변경
- [ ] 제목 "최근 응대 현황"으로 변경
- [ ] answerStatus Badge 표시

---

## 3. introduce-domain-events — Spring Application Event 도입

**Change**: `openspec/changes/introduce-domain-events/`
**상태**: `planned` — 미시작

### P1: 이벤트 클래스 생성

- [ ] `modules/chat-runtime/.../domain/QuestionAnsweredEvent.kt`
- [ ] `modules/ingestion-ops/.../domain/IngestionJobCompletedEvent.kt`
- [ ] `modules/qa-review/.../domain/QAReviewResolvedEvent.kt`

### P2: 서비스에 이벤트 발행 추가

- [ ] `CreateQuestionService` — 답변 저장 후 `QuestionAnsweredEvent` 발행
- [ ] `TransitionJobService` — success/failed 전이 후 `IngestionJobCompletedEvent` 발행
- [ ] `CreateQAReviewService` — resolved/false_alarm 시 `QAReviewResolvedEvent` 발행
- [ ] `ServiceConfiguration` — 각 서비스에 `ApplicationEventPublisher` 주입 추가

### P3: 이벤트 핸들러 생성

- [ ] `QuestionAnsweredEventHandler.kt` (qa-review 모듈)
- [ ] `IngestionJobCompletedEventHandler.kt` (metrics-reporting 모듈)
- [ ] `QAReviewResolvedEventHandler.kt` (metrics-reporting 모듈)
- [ ] `RepositoryConfiguration` — 핸들러 빈 등록

### P4: 검증

- [ ] 기존 50개 테스트 통과 확인
- [ ] ArchUnit 8개 규칙 통과 확인
