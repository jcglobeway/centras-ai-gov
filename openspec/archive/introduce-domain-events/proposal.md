# Proposal

## Change ID

`introduce-domain-events`

## Summary

- **변경 목적**: 바운디드 컨텍스트 간 사이드 이펙트를 직접 호출/외부 배치 대신 DomainEvent + Spring ApplicationEventPublisher로 처리. `shared-kernel/DomainEvent.kt` 마커 인터페이스를 실제 활용.
- **변경 범위**: 3개 이벤트 클래스 신규 생성, 3개 이벤트 핸들러 신규 생성, 3개 서비스에 발행 로직 추가
- **제외 범위**: RAGAS/LLM-as-Judge 연동, 외부 메시지 브로커(Redis/Kafka), Python 서비스 연동

## Cross-Context 문제

| 트리거 | 기대 사이드 이펙트 | 현재 처리 | 문제 |
|--------|------------------|-----------|------|
| 답변 생성 완료 (fallback/no_answer/error) | QA 리뷰 자동 생성 | 없음 | 미응답 큐 수동 관리 |
| 인제스션 잡 success/failed 전이 | 지표 KPI 갱신 | 없음 | 인덱싱 지표 실시간성 없음 |
| QA 리뷰 resolved/false_alarm 전이 | 해결율 재계산 | 없음 | 지표 정합성 없음 |

## 이벤트 설계

### Event 1: `QuestionAnsweredEvent` (chat-runtime/domain)

```kotlin
data class QuestionAnsweredEvent(
    val questionId: String,
    val organizationId: String,
    val serviceId: String,
    val answerStatus: AnswerStatus,
    val failureReasonCode: FailureReasonCode?,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
```

- 발행: `CreateQuestionService` — 답변 저장 완료 후 (ragResult != null 이면 발행, null이면 미발행)
- 구독: `qa-review/adapter/inbound/event/QuestionAnsweredEventHandler` — `answerStatus ∈ {FALLBACK, NO_ANSWER, ERROR}` 이면 `pending` QA 리뷰 자동 생성

### Event 2: `IngestionJobCompletedEvent` (ingestion-ops/domain)

```kotlin
data class IngestionJobCompletedEvent(
    val jobId: String,
    val organizationId: String,
    val serviceId: String,
    val success: Boolean,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
```

- 발행: `TransitionJobService` — 전이 결과 status가 `success` 또는 `failed` 일 때
- 구독: `metrics-reporting/adapter/inbound/event/IngestionJobCompletedEventHandler` — 해당 org/service의 인덱싱 KPI 스냅샷 갱신

### Event 3: `QAReviewResolvedEvent` (qa-review/domain)

```kotlin
data class QAReviewResolvedEvent(
    val reviewId: String,
    val questionId: String,
    val organizationId: String,
    val finalStatus: String,   // "resolved" | "false_alarm"
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
```

- 발행: `CreateQAReviewService` — 리뷰 상태가 `resolved` 또는 `false_alarm` 일 때
- 구독: `metrics-reporting/adapter/inbound/event/QAReviewResolvedEventHandler` — 해당 날짜 resolvedRate 재계산 트리거

## 인프라 전략

Spring `ApplicationEventPublisher` (인-프로세스). 핸들러는 `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용. 추가 의존성 없음.

서비스 생성자에 `ApplicationEventPublisher` 주입 → `ServiceConfiguration`에서 Spring이 자동으로 주입.

## 아키텍처 규칙 준수

- 이벤트 클래스는 각 컨텍스트의 `domain/` 패키지에 위치
- 핸들러는 구독자 컨텍스트의 `adapter/inbound/event/` 패키지에 위치
- 핸들러 내에서 UseCase 인터페이스를 통해서만 도메인 조작 (직접 포트 접근 금지)
- ArchUnit 규칙 충돌 없음 — `adapter.inbound.event`는 기존 규칙 범위 외

## Impact

- **영향 모듈**: chat-runtime, ingestion-ops, qa-review, metrics-reporting, admin-api(config)
- **영향 API**: 없음 (내부 사이드 이펙트 추가, API 응답 변경 없음)
- **영향 테스트**: 기존 50개 테스트 영향 없음. 이벤트 발행 검증용 통합 테스트 추가 권장.

## Done Definition

- [ ] `QuestionAnsweredEvent` 발행 시 `qa_reviews`에 `pending` 리뷰가 자동 생성됨
- [ ] `IngestionJobCompletedEvent` 발행 시 `daily_metrics_org` KPI가 갱신됨
- [ ] `QAReviewResolvedEvent` 발행 시 `resolvedRate`가 재계산됨
- [ ] 기존 50개 통합 테스트 모두 통과
- [ ] ArchUnit 8개 규칙 모두 통과
