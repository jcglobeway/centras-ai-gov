package com.publicplatform.ragops.metricsreporting

/**
 * 기관별 일간 KPI 스냅샷을 관리하는 바운디드 컨텍스트.
 *
 * 사전 집계된 지표를 저장하며 온디맨드 집계는 수행하지 않는다.
 * 배치 작업이 매일 원본 로그에서 집계하여 이 모듈의 테이블에 저장한다.
 *
 * ## 주요 포트 계약
 * - [MetricsReader]: 일간 지표 목록 조회 (날짜 범위, 기관 필터)
 *
 * ## KPI 지표 항목
 * - `total_questions`: 총 질문 수
 * - `answered_count`: 답변 완료 수
 * - `fallback_count`: 폴백 응답 수
 * - `no_answer_count`: 답변 없음 수
 * - `avg_response_time_ms`: 평균 응답 시간 (ms)
 * - `unresolved_count`: 미해결 질문 수
 *
 * ## 주요 DB 테이블
 * - `daily_metrics_org`
 *
 * ## 의존 관계
 * - organization-directory: 기관 스코프 필터
 * - chat-runtime의 원본 데이터를 기반으로 배치 집계 (직접 의존하지 않음)
 */
class MetricsReportingModule
