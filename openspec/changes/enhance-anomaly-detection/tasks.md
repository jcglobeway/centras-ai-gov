# Tasks — enhance-anomaly-detection

## DB (Flyway)

- [ ] T1: `V033__create_anomaly_tables.sql` 작성
  - `alert_thresholds(metric_key VARCHAR PK, warn_value DOUBLE, critical_value DOUBLE, updated_at TIMESTAMP)`
  - `alert_events(id VARCHAR PK, metric_key VARCHAR, current_value DOUBLE, severity VARCHAR, triggered_at TIMESTAMP)`
  - 기본 임계값 시드: fallback_rate(warn=10, critical=15), zero_result_rate(warn=5, critical=8), avg_response_time_ms(warn=1500, critical=2500)

## 백엔드 — 도메인 모델

- [ ] T2: `AnomalyThreshold.kt` 도메인 모델 (metrics-reporting/domain)
  - `data class AnomalyThreshold(val metricKey: String, val warnValue: Double, val criticalValue: Double)`

- [ ] T3: `AlertEvent.kt` 도메인 모델 (metrics-reporting/domain)
  - `data class AlertEvent(val id: String, val metricKey: String, val currentValue: Double, val severity: String, val triggeredAt: Instant)`

## 백엔드 — UseCase 인터페이스

- [ ] T4: `GetAnomalyThresholdsUseCase`, `UpdateAnomalyThresholdsUseCase` (port/in)
- [ ] T5: `GetAlertEventsUseCase`, `GetDriftSummaryUseCase` (port/in)
  - `GetDriftSummaryUseCase`: 최근 14일 daily_metrics 조회 → 7일 rolling avg + deviation 계산

## 백엔드 — 영속성 어댑터

- [ ] T6: JPA Entity + JpaRepository (alert_thresholds, alert_events)
- [ ] T7: Port Adapter 구현체 (LoadAnomalyThresholdPortAdapter, SaveAnomalyThresholdPortAdapter, LoadAlertEventPortAdapter, SaveAlertEventPortAdapter)

## 백엔드 — 서비스

- [ ] T8: 서비스 구현체 4개
  - `GetAnomalyThresholdsService`, `UpdateAnomalyThresholdsService`
  - `GetAlertEventsService`, `GetDriftSummaryService`

## 백엔드 — Controller + Scheduler

- [ ] T9: `AnomalyController.kt` 신규 (metrics/adapter/inbound/web)
  - `GET /admin/anomaly/thresholds`
  - `PUT /admin/anomaly/thresholds` (body: List<{metricKey, warnValue, criticalValue}>)
  - `GET /admin/anomaly/drift-summary`
  - `GET /admin/anomaly/alert-events`

- [ ] T10: `AnomalyAlertScheduler.kt` 신규 (metrics/adapter/inbound/scheduler)
  - `@Scheduled(fixedRate = 3_600_000)` — 1시간마다 실행
  - 최신 daily_metrics 조회 → alert_thresholds 비교 → 초과 시 alert_events 삽입

- [ ] T11: `ServiceConfiguration` / `RepositoryConfiguration` 빈 등록

## 프론트엔드

- [ ] T12: `anomaly/page.tsx` — 임계값 저장 API 연동
  - `GET /api/admin/anomaly/thresholds` → 초기값 로드
  - 저장 버튼 → `PUT /api/admin/anomaly/thresholds` 호출 + 성공 토스트

- [ ] T13: `anomaly/page.tsx` — drift-summary API 연동
  - 클라이언트 계산 로직 제거 → `GET /api/admin/anomaly/drift-summary` 결과 표시

- [ ] T14: `anomaly/page.tsx` — 알림 이력 테이블 추가
  - `GET /api/admin/anomaly/alert-events` 연동
  - 컬럼: 지표, 발생값, 심각도, 발생 시각

## 테스트

- [ ] T15: `AnomalyApiTests.kt` 신규
  - 임계값 GET → 기본 시드 3건 반환 확인
  - 임계값 PUT → GET으로 재조회 시 반영 확인
  - drift-summary → 정상 응답 구조 확인
  - alert-events → 목록 반환 확인
  - `./gradlew test` 전체 통과 확인
