# Status: metrics-aggregation-trigger

## 현재 상태: IMPLEMENTED

## 변경 이력

| 날짜 | 상태 | 내용 |
|------|------|------|
| 2026-04-02 | DRAFT | proposal, tasks 작성 |
| 2026-04-02 | IN_PROGRESS | 구현 시작 |
| 2026-04-02 | IMPLEMENTED | 50개 테스트 전체 통과, 구현 완료 |

## 구현 완료 내역

### Phase 1 — 스케줄 주기 단축
- `application.yml`: `metrics.aggregation.cron: "0 */30 * * * *"` 추가
- `MetricsAggregationScheduler.kt`: cron 값을 `${metrics.aggregation.cron:0 5 0 * * *}`으로 외부화

### Phase 2 — UseCase 계층 추가
- `TriggerMetricsAggregationUseCase` 인터페이스 신규 생성
- `TriggerMetricsAggregationService` 구현체 신규 생성
- `ServiceConfiguration`에 `triggerMetricsAggregationUseCase` Bean 등록

### Phase 3 — MetricsController 정비
- `MetricsAggregationScheduler` 직접 의존 제거 → `TriggerMetricsAggregationUseCase`로 교체
- 엔드포인트: `POST /admin/metrics/aggregate` → `POST /admin/metrics/trigger-aggregation`
- `date` 기본값: 전날 → 오늘
- 권한 체크: `metrics.aggregation.trigger` 액션 추가
- Response: `TriggerAggregationResponse(date, triggeredAt)` 신규 정의

### 부가 수정
- `RagasEvaluationController`: `citationCoverage`/`citationCorrectness` 컴파일 오류 수정
- V038 중복 마이그레이션 → V040/V041로 번호 변경
- `DevelopmentAdminSessionReader.kt`: `ops_admin`, `super_admin`에 `metrics.aggregation.trigger` 액션 추가
