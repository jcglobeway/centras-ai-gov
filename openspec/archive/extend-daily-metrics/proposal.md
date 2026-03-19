# Proposal

## Change ID

`extend-daily-metrics`

## Summary

- **변경 목적**: `daily_metrics_org` 테이블에 planning_draft 기준 KPI 지표 컬럼 추가
- **변경 범위**:
  - DB: V023 마이그레이션 — `daily_metrics_org` 컬럼 추가
  - 도메인: `DailyMetrics` 도메인 모델 업데이트
  - API: `/admin/daily-metrics` 응답에 신규 지표 포함
- **제외 범위**: 실제 집계 배치 로직, 자동 계산 스케줄러

## 추가 컬럼 상세

### daily_metrics_org 테이블
| 컬럼 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `auto_resolution_rate` | DECIMAL(5,4) | NULL | 자동응대 완료율 |
| `escalation_rate` | DECIMAL(5,4) | NULL | 상담 전환율 |
| `explicit_resolution_rate` | DECIMAL(5,4) | NULL | 명시적 해결율 (피드백 기반) |
| `estimated_resolution_rate` | DECIMAL(5,4) | NULL | 추정 해결율 (행동 신호 기반) |
| `revisit_rate` | DECIMAL(5,4) | NULL | 재문의율 (7일 내 동일 질문) |
| `after_hours_rate` | DECIMAL(5,4) | NULL | 업무시간 외 응대 비율 |
| `avg_session_turn_count` | DECIMAL(5,2) | NULL | 평균 대화 턴 수 |
| `knowledge_gap_count` | INTEGER | 0 | 지식 공백 건수 (문서 없음) |
| `unanswered_count` | INTEGER | 0 | 미응답 질문 수 |
| `low_satisfaction_count` | INTEGER | 0 | 저만족 응답 수 |

## Impact

- **영향 모듈**: `metrics-reporting`, `apps/admin-api`
- **영향 API**: `GET /admin/daily-metrics`
- **영향 테스트**: `ChatRuntimeApiTests` (metrics 관련 케이스)

## Done Definition

- [ ] V023 마이그레이션 작성 (ALTER TABLE daily_metrics_org)
- [ ] `DailyMetricsEntity` 신규 컬럼 매핑
- [ ] `DailyMetrics` 도메인 모델 필드 추가
- [ ] API 응답 DTO 업데이트
- [ ] `./gradlew test` 전체 통과
