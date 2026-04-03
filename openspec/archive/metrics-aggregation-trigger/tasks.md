# Tasks: metrics-aggregation-trigger

## Phase 1 — 스케줄 주기 단축 및 외부화

- [x] T1: `application.yml`에 `metrics.aggregation.cron` 설정 추가
- [x] T2: `MetricsAggregationScheduler.kt` cron 값을 프로퍼티로 교체

## Phase 2 — 온디맨드 트리거 UseCase 계층 추가

- [x] T3: `TriggerMetricsAggregationUseCase` 인터페이스 작성
- [x] T4: `TriggerMetricsAggregationService` 구현체 작성
- [x] T5: `ServiceConfiguration`에 `TriggerMetricsAggregationService` Bean 등록

## Phase 3 — MetricsController 정비

- [x] T6: `MetricsController`의 스케줄러 직접 의존 제거, UseCase 인터페이스로 교체
- [x] T7: 엔드포인트 경로를 `/admin/metrics/trigger-aggregation`으로 변경
- [x] T8: `date` 파라미터 기본값을 오늘 날짜로 변경 (기존: 전날)
- [x] T9: `ops_admin`, `super_admin` 권한 체크 추가
- [x] T10: `TriggerAggregationResponse`에 `triggeredAt` 필드 추가

## 부가 수정 (발견된 기존 문제)

- [x] B1: `RagasEvaluationController`의 `citationCoverage`/`citationCorrectness` 컴파일 오류 수정
- [x] B2: V038 중복 마이그레이션 파일 충돌 해소 (V040, V041로 번호 변경)
- [x] B3: `DevelopmentAdminSessionReader.kt`에 `metrics.aggregation.trigger` 액션 추가

## Testing

- [x] T11: `./gradlew :apps:admin-api:test` 50개 테스트 전체 통과 확인
