# enhance-anomaly-detection — Tasks

## 백엔드
- [x] V045 Flyway 마이그레이션 (`alert_thresholds`, `alert_events` + 기본 시드 3건)
- [x] 도메인 모델: `AnomalyThreshold`, `AlertEvent`, `DriftSummary`, `UpdateThresholdCommand`
- [x] UseCase: `GetAnomalyThresholdsUseCase`, `UpdateAnomalyThresholdsUseCase`, `GetAlertEventsUseCase`, `GetDriftSummaryUseCase`
- [x] Port/out: `LoadAnomalyThresholdPort`, `SaveAnomalyThresholdPort`, `LoadAlertEventPort`, `SaveAlertEventPort`
- [x] JPA 엔티티 + Repository + PortAdapter (4개 포트 구현)
- [x] Service 4개: `GetAnomalyThresholdsService`, `UpdateAnomalyThresholdsService`, `GetAlertEventsService`, `GetDriftSummaryService`
- [x] `AnomalyController`: GET/PUT /admin/anomaly/thresholds, GET /admin/anomaly/alert-events, GET /admin/anomaly/drift-summary
- [x] `AnomalyAlertScheduler`: @Scheduled(fixedRate=3_600_000), 임계값 초과 시 AlertEvent 저장
- [x] RepositoryConfiguration / ServiceConfiguration 수정

## 프론트엔드
- [x] anomaly/page.tsx — 클라이언트 계산 제거, API 연동, 알림 이력 테이블, 저장 버튼 활성화

## 검증
- [x] compileKotlin 통과
- [x] 전체 테스트 통과
