# Proposal

## Change ID

`enhance-anomaly-detection`

## Context (왜 이 기능이 중요한가)

이 페이지의 목적은 **조용히 나빠지고 있는 것을 조기에 포착하는 것**이다. 사전 예방(proactive monitoring) 성격이 강하다.

세 지표 중 **Query Drift가 핵심**이다. Query Drift가 다른 모든 품질 지표 하락의 선행 신호이기 때문이다.

```
사용자 질문 패턴 변화 (Query Drift 상승)
  → Recall@K 하락
  → Faithfulness 하락
  → Session Success Rate 하락
```

Drift를 일찍 잡으면 나머지 지표가 나빠지기 전에 대응할 수 있다.

**Recall Baseline Deviation**은 2순위다. Query Drift가 "패턴이 바뀌고 있다"는 신호라면, Recall Deviation은 "이미 검색 품질이 나빠지기 시작했다"는 신호다. 두 지표가 동시에 경고 상태면 즉각 조치가 필요한 상황이다.

Embedding Drift와 비정상 반복 질의는 중요하지만 빈도가 낮아 이상 시에만 보게 되는 지표다.

## Summary

- **목적**: 이상 징후 감지 페이지의 핵심 기능(임계값 저장, 서버 사이드 드리프트 계산, 알림 이력)을 실제로 동작하도록 구현한다.
- **배경**: `/ops/anomaly` 페이지는 이미 존재하지만 임계값 저장 버튼이 비활성화 상태이고, Drift 계산이 클라이언트 단 단순 비율 계산에 머물러 있다. 알림 이력 테이블도 없어서 "언제 임계값을 초과했는가"를 추적할 수 없다.
- **변경 범위**:
  - DB: `alert_thresholds`, `alert_events` 테이블 신규 (Flyway V033)
  - 백엔드: `GET/PUT /admin/anomaly/thresholds`, `GET /admin/anomaly/drift-summary`, `GET /admin/anomaly/alert-events`
  - 스케줄러: 1시간마다 daily_metrics 체크 → 임계값 초과 시 `alert_events` 기록
  - 프론트: 임계값 저장 버튼 활성화, 알림 이력 테이블 추가
- **제외 범위**:
  - Webhook/Slack/PagerDuty 연동 (별도 P1 작업)
  - 임베딩 centroid cosine 드리프트 계산 (Python 서비스 필요, P2)
  - Query Novelty Rate / KMeans 클러스터링 (P3)

## Impact

- **영향 모듈**: `modules/metrics-reporting` (신규 UseCase·Service·Adapter 추가), `apps/admin-api` (Controller, Scheduler, Flyway)
- **영향 API**:
  - `GET /admin/anomaly/thresholds` — 신규
  - `PUT /admin/anomaly/thresholds` — 신규
  - `GET /admin/anomaly/drift-summary` — 신규 (7일 rolling avg, deviation 서버 계산)
  - `GET /admin/anomaly/alert-events` — 신규
- **영향 파일**:
  - `apps/admin-api/src/main/resources/db/migration/V033__create_anomaly_tables.sql`
  - `modules/metrics-reporting/src/.../domain/AnomalyThreshold.kt` (신규)
  - `modules/metrics-reporting/src/.../domain/AlertEvent.kt` (신규)
  - `modules/metrics-reporting/src/.../application/port/in/GetAnomalyThresholdsUseCase.kt` (신규)
  - `modules/metrics-reporting/src/.../application/port/in/UpdateAnomalyThresholdsUseCase.kt` (신규)
  - `modules/metrics-reporting/src/.../application/port/in/GetAlertEventsUseCase.kt` (신규)
  - `modules/metrics-reporting/src/.../application/port/in/GetDriftSummaryUseCase.kt` (신규)
  - `modules/metrics-reporting/src/.../application/service/` (각 UseCase 구현체, 신규)
  - `modules/metrics-reporting/src/.../adapter/outbound/persistence/` (Entity, JpaRepository, Adapter, 신규)
  - `apps/admin-api/src/.../metrics/adapter/inbound/web/AnomalyController.kt` (신규)
  - `apps/admin-api/src/.../metrics/adapter/inbound/scheduler/AnomalyAlertScheduler.kt` (신규)
  - `apps/admin-api/src/.../config/ServiceConfiguration.kt` (빈 등록)
  - `apps/admin-api/src/.../config/RepositoryConfiguration.kt` (빈 등록)
  - `frontend/src/app/ops/anomaly/page.tsx`
- **영향 테스트**: 신규 `AnomalyApiTests.kt`

## Done Definition

- `PUT /admin/anomaly/thresholds` → DB 저장, `GET`으로 재조회 시 반영 확인
- `GET /admin/anomaly/drift-summary` → fallbackRate·zeroResultRate 7일 rolling avg, deviation 계산값 반환
- `GET /admin/anomaly/alert-events` → 임계값 초과 이벤트 목록 반환
- 스케줄러: 1시간마다 실행, 임계값 초과 시 `alert_events` 행 삽입
- 프론트: 임계값 입력 → 저장 → DB 반영 (toast 피드백)
- 프론트: 알림 이력 테이블에 `alert_events` 실데이터 표시
- 통합 테스트 추가 통과 (`./gradlew test`)
