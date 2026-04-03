# daily_metrics_org 집계 명세

`MetricsAggregationScheduler`가 `questions` + `answers` + `feedbacks` 테이블을 기관·서비스 단위로 집계하여 `daily_metrics_org`에 upsert한다.

---

## 집계 트리거

| 방식 | 내용 |
|------|------|
| **Scheduled** | `metrics.aggregation.cron` 설정값 (기본: `0 5 0 * * *`, 현재 운영: `0 */30 * * * *`) |
| **On-request** | `POST /admin/metrics/trigger-aggregation?date=YYYY-MM-DD` (ops_admin / super_admin 전용) |

집계 대상일: Scheduled는 전날(`now() - 1`), On-request는 쿼리 파라미터 `date` (생략 시 어제).

---

## 소스 테이블 및 집계 항목

### questions + answers (메인 집계)

`questions`와 `answers`를 LEFT JOIN하여 `q.created_at` 기준 대상일 필터.

| 컬럼 | 계산식 | 단위 |
|------|--------|------|
| `total_sessions` | `COUNT(DISTINCT q.chat_session_id)` | 세션 수 |
| `total_questions` | `COUNT(q.id)` | 질문 수 |
| `resolved_rate` | `answered_count / total_questions × 100` | % |
| `fallback_rate` | `fallback_count / total_questions × 100` | % |
| `zero_result_rate` | `no_answer_count / total_questions × 100` | % |
| `avg_response_time_ms` | `AVG(a.response_time_ms)` | ms |
| `auto_resolution_rate` | `(answer_status='answered' AND is_escalated=false) / total_questions` | 0–1 |
| `escalation_rate` | `is_escalated=true / total_questions` | 0–1 |
| `estimated_resolution_rate` | `answered_count / total_questions` | 0–1 |
| `after_hours_rate` | `(hour < 9 OR hour ≥ 18) / total_questions` | 0–1 |
| `avg_session_turn_count` | `AVG(세션별 질문 수)` — 서브쿼리 | 회 |
| `knowledge_gap_count` | `failure_reason_code = 'A01'` 건수 | 건 |
| `unanswered_count` | `fallback + no_answer + escalated` 건수 | 건 |

> `resolved_rate`와 `estimated_resolution_rate`는 분모·분자가 동일하다.
> 전자는 `× 100` 퍼센트, 후자는 0–1 비율로 저장된다.

### feedbacks (별도 집계)

`feedbacks.submitted_at` 기준 대상일 필터. 메인 집계와 `organization_id + service_id`로 조인.

| 컬럼 | 계산식 | 단위 |
|------|--------|------|
| `explicit_resolution_rate` | `target_action_completed=true / feedback_count` (피드백 0건이면 null) | 0–1 |
| `revisit_rate` | `피드백이 있는 고유 세션 수 / total_sessions` (total_sessions=0이면 null) | 0–1 |
| `low_satisfaction_count` | `rating ≤ 2` 건수 | 건 |

---

## 0-행 보장

활성 기관(`organizations.status = 'active'`)의 모든 서비스에 대해, 해당일 질문이 없더라도 모든 카운트를 0으로 채운 행을 생성한다. 대시보드 시계열 차트의 빈 날짜 구간을 방지하기 위함이다.

---

## 포함되지 않는 지표

| 지표 | 저장 위치 | 조회 방법 |
|------|----------|----------|
| faithfulness, answer_relevancy 등 RAGAS 품질 지표 | `ragas_evaluations` | `GET /admin/ragas-evaluations/summary` |
| latency_ms, llm_ms 등 검색 단계 시간 | `rag_search_logs` | `GET /admin/rag-search-logs` |
| confidence_score, citation_count | `answers` | 직접 쿼리 또는 별도 API |

`daily_metrics_org`는 **운영 KPI** (세션·질문량, 응답률, 에스컬레이션, 만족도)만 담는다.

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `MetricsAggregationScheduler.kt` | 집계 SQL 실행 + SaveMetricsPort 호출 |
| `MetricsController.kt` | `POST /admin/metrics/trigger-aggregation` 수동 트리거 |
| `SaveDailyMetricsCommand.kt` | 집계 결과 전달 커맨드 |
| `SaveMetricsPortAdapter.kt` | `daily_metrics_org` upsert 실행 |
| `V015__create_daily_metrics_org.sql` | 테이블 생성 |
| `V023__extend_daily_metrics_org.sql` | 컬럼 확장 (auto_resolution_rate, escalation_rate 등 7개) |
