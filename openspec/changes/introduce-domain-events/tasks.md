# Tasks

## P1: 이벤트 클래스 생성

- [x] `modules/chat-runtime/.../domain/QuestionAnsweredEvent.kt`
- [x] `modules/ingestion-ops/.../domain/IngestionJobCompletedEvent.kt`
- [x] `modules/qa-review/.../domain/QAReviewResolvedEvent.kt`

## P2: 서비스에 이벤트 발행 추가

- [x] `CreateQuestionService` — 답변 저장 후 `QuestionAnsweredEvent` 발행 (`ApplicationEventPublisher` 주입)
- [x] `TransitionJobService` — success/failed 전이 후 `IngestionJobCompletedEvent` 발행
- [x] `CreateQAReviewService` — resolved/false_alarm 시 `QAReviewResolvedEvent` 발행
- [x] `ServiceConfiguration` — 각 서비스에 `ApplicationEventPublisher` 주입 추가

## P3: 이벤트 핸들러 생성

- [x] `modules/qa-review/.../adapter/inbound/event/QuestionAnsweredEventHandler.kt`
- [x] `apps/admin-api/.../metrics/adapter/inbound/event/IngestionJobCompletedEventHandler.kt`
- [x] `apps/admin-api/.../metrics/adapter/inbound/event/QAReviewResolvedEventHandler.kt`
- [x] `RepositoryConfiguration` — 핸들러 빈 등록

## P4: 검증

- [x] 기존 50개 테스트 통과 확인
- [x] ArchUnit 8개 규칙 통과 확인
- [ ] 이벤트 발행 통합 테스트 작성 (선택)

## P5: 정리

- [x] 커밋 (한국어 메시지)
