# Proposal: metrics-aggregation-trigger

## Problem

`MetricsAggregationScheduler`가 매일 새벽 00:05에만 실행되어 `daily_metrics_org`
데이터가 다음날에야 반영된다. 이로 인해 대시보드에서 당일 질문 투입 후 즉시 지표
확인이 불가능하다.

추가로 현재 `MetricsController`가 `MetricsAggregationScheduler`를 직접 참조하는
구조는 헥사고날 아키텍처를 위반한다. Controller는 UseCase 인터페이스에만 의존해야
한다.

## Proposed Solution

1. **스케줄 주기 단축**: 30분마다 자동 집계 (기존 1일 1회 → 30분)
   - cron 값을 `application.yml`로 외부화하여 환경별 설정 가능하게 변경

2. **온디맨드 트리거 API 정비**: 기존 `POST /admin/metrics/aggregate` 엔드포인트를
   헥사고날 아키텍처에 맞게 재구성
   - `TriggerMetricsAggregationUseCase` 인터페이스 추가
   - `TriggerMetricsAggregationService` 구현체 추가
   - `MetricsController`의 스케줄러 직접 의존 제거 → UseCase 인터페이스로 교체
   - 권한 체크(`ops_admin`, `super_admin`) 추가
   - Response에 `triggeredAt` 필드 추가

## Out of Scope

- 집계 결과 알림 기능
- 집계 이력 저장
- Flyway 마이그레이션 변경

## Success Criteria

- `POST /admin/metrics/trigger-aggregation` 호출 시 즉시 집계 실행
- 30분마다 자동 집계 실행
- `ops_admin`, `super_admin` 이외 역할은 403 반환
- 기존 50개 테스트 전체 통과
