# Status

- 상태: `planned`
- 시작일: `2026-04-03`
- 마지막 업데이트: `2026-04-03`

## Progress

- proposal 작성 완료

## Verification

- 미실행

## Risks

- `GetDriftSummaryService`가 `LoadDailyMetricsPort`에 의존 — metrics-reporting 모듈 내 기존 포트 재사용 또는 신규 추가 필요
- `AnomalyAlertScheduler`의 중복 이벤트 방지 로직 필요 (같은 시간에 여러 번 삽입 방지)
- H2 테스트 환경 flyway.target을 V033까지 올려야 함
