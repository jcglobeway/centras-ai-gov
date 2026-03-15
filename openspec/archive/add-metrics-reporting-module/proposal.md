# Proposal

## Change ID

`add-metrics-reporting-module`

## Summary

### 변경 목적
- Metrics Reporting 모듈 구현 (KPI 집계)
- daily_metrics_org 테이블로 Dashboard 데이터 제공
- MVP 전체 모듈 완성!

### 변경 범위
- Flyway migration: V015 daily_metrics_org
- metrics-reporting JPA 구현
- Dashboard API

### 제외 범위
- 실시간 집계 로직 (별도 배치)
- 차트 데이터 가공

## Done Definition

- [ ] Flyway migration
- [ ] JPA 엔티티 + Repository
- [ ] API
- [ ] 테스트
- [ ] ./gradlew test 통과
