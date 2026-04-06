# enhance-anomaly-detection — Proposal

## 배경

이상 징후 감지 페이지의 임계값과 알림 이벤트를 DB 기반으로 관리하고,
드리프트 요약 지표를 백엔드 API로 제공한다.

## 범위

### 백엔드 (admin-api + metrics-reporting)
- V045 Flyway 마이그레이션: `alert_thresholds`, `alert_events` 테이블 + 기본값 시드
- 도메인 모델: `AnomalyThreshold`, `AlertEvent`
- UseCase (port/in): Get/Update Threshold, GetAlertEvents, GetDriftSummary
- 아웃바운드 포트 (port/out): Load/Save Threshold, Load/Save AlertEvent
- JPA 엔티티, Repository, Port Adapter, Service
- AnomalyController: GET/PUT /admin/anomaly/thresholds, GET /admin/anomaly/drift-summary, GET /admin/anomaly/alert-events
- AnomalyAlertScheduler: 매시간 임계값 체크 → AlertEvent 저장
- Bean 등록: RepositoryConfiguration, ServiceConfiguration 수정

### 프론트엔드
- anomaly/page.tsx — 클라이언트 계산 → API 연동 전환
