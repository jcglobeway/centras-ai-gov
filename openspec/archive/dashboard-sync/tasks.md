# Tasks

## Phase 1 — QuestionResponse 확장 (백엔드)

- [ ] `QuestionResponse`에 `failureReasonCode`, `questionCategory`, `isEscalated`, `answerConfidence` 필드 추가
- [ ] `toResponse()` 매핑 4개 필드 추가
- [ ] 테스트: GET /admin/questions 응답 검증
- [ ] 커밋: `기능: QuestionResponse 필드 확장 (failureReasonCode, isEscalated 등)`

## Phase 2 — 미결질문 answerStatus + latestReviewStatus 추가 (백엔드)

- [ ] `UnresolvedRow` projection 인터페이스 + `findUnresolvedWithStatus()` native query 추가
- [ ] `UnresolvedQuestionSummary` 도메인 모델 추가
- [ ] `LoadQuestionPort.listUnresolvedQuestions()` 반환 타입 변경
- [ ] `ListQuestionsUseCase.listUnresolved()` 반환 타입 변경
- [ ] `ListQuestionsService.listUnresolved()` 구현 교체
- [ ] `LoadQuestionPortAdapter.listUnresolvedQuestions()` 구현 교체
- [ ] `UnresolvedQuestionResponse` DTO 신규 + `QuestionController.listUnresolvedQuestions()` 교체
- [ ] 기존 `findUnresolvedQuestions()` 삭제 (사용처 없음 확인)
- [ ] 테스트 50개 통과 확인
- [ ] 커밋: `기능: 미결질문 응답에 answerStatus, latestReviewStatus 추가`

## Phase 3 — 프론트엔드 타입 동기화

- [ ] `types.ts` `Question` 인터페이스 필드명 백엔드 일치 (sessionId→chatSessionId, failureCode→failureReasonCode, wasTransferred→isEscalated, categoryL1/L2→questionCategory)
- [ ] `types.ts` `UnresolvedQuestion` 인터페이스 추가 (answerStatus, latestReviewStatus 포함)
- [ ] `types.ts` `DailyMetric` V023 7개 필드 추가
- [ ] 커밋: `리팩토링: 프론트엔드 타입 백엔드 API와 동기화`

## Phase 4 — 프론트엔드 페이지 필드명 수정

- [ ] `client/failure/page.tsx`: `q.failureCode` → `q.failureReasonCode`
- [ ] `qa/page.tsx`: `q.failureCode` → `q.failureReasonCode`
- [ ] `qa/unresolved/page.tsx`: `UnresolvedQuestion` 타입 사용
- [ ] `ops/page.tsx`: answerStatus가 없는 Question 타입 처리 수정
- [ ] 커밋: `수정: 프론트엔드 페이지 필드명 백엔드 일치`

## Phase 5 — QA Review review_status 필터 + confirmedCount 수정

- [ ] `JpaQAReviewRepository.findByReviewStatus(status: String)` 추가
- [ ] `LoadQAReviewPort.listByStatus()` 추가
- [ ] `ListQAReviewsUseCase.listByStatus()` 추가
- [ ] `ListQAReviewsService.listByStatus()` 구현
- [ ] `QAReviewController.listQAReviews()` `review_status` 파라미터 추가
- [ ] `qa/page.tsx` confirmedCount를 `total` 기반으로 수정
- [ ] 테스트 50개 통과 확인
- [ ] 커밋: `기능: QA리뷰 review_status 필터 추가 및 confirmedCount 수정`

## Phase 6 — V023 온디맨드 집계

- [ ] `DailyMetricsSummary` V023 7개 필드 추가
- [ ] `LoadDailyMetricsPortAdapter.aggregateFromRawData()` native SQL 집계 구현
- [ ] `DailyMetricsResponse` V023 필드 추가 + `toResponse()` 매핑
- [ ] `client/page.tsx` V023 필드 사용 (autoResolutionRate, escalationRate 등)
- [ ] 테스트 50개 통과 확인
- [ ] 커밋: `기능: DailyMetrics V023 필드 온디맨드 집계 추가`

## Phase 7 — client/performance/page.tsx 교체

- [ ] `/admin/questions` API 사용하도록 변경 (unresolved 대신)
- [ ] 제목 "최근 응대 현황"으로 변경
- [ ] answerStatus Badge 표시
- [ ] 커밋: `리팩토링: client/performance 최근 응대 현황으로 교체`
