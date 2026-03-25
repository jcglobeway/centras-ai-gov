# Tasks

## P1: 이벤트 클래스 생성

- [ ] `modules/chat-runtime/.../domain/QuestionAnsweredEvent.kt`
- [ ] `modules/ingestion-ops/.../domain/IngestionJobCompletedEvent.kt`
- [ ] `modules/qa-review/.../domain/QAReviewResolvedEvent.kt`

## P2: 서비스에 이벤트 발행 추가

- [ ] `CreateQuestionService` — 답변 저장 후 `QuestionAnsweredEvent` 발행 (`ApplicationEventPublisher` 주입)
- [ ] `TransitionJobService` — success/failed 전이 후 `IngestionJobCompletedEvent` 발행
- [ ] `CreateQAReviewService` — resolved/false_alarm 시 `QAReviewResolvedEvent` 발행
- [ ] `ServiceConfiguration` — 각 서비스에 `ApplicationEventPublisher` 주입 추가

## P3: 이벤트 핸들러 생성

- [ ] `modules/qa-review/.../adapter/inbound/event/QuestionAnsweredEventHandler.kt`
- [ ] `modules/metrics-reporting/.../adapter/inbound/event/IngestionJobCompletedEventHandler.kt`
- [ ] `modules/metrics-reporting/.../adapter/inbound/event/QAReviewResolvedEventHandler.kt`
- [ ] `RepositoryConfiguration` — 핸들러 빈 등록

## P4: 검증

- [ ] 기존 50개 테스트 통과 확인
- [ ] ArchUnit 8개 규칙 통과 확인
- [ ] 이벤트 발행 통합 테스트 작성 (선택)

## P5: 정리

- [ ] 커밋 (한국어 메시지)
