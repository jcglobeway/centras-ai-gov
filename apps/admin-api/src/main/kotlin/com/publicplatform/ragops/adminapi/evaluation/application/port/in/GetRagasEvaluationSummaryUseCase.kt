package com.publicplatform.ragops.adminapi.evaluation.application.port.`in`

import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationAggregate
import java.time.LocalDate

/**
 * RAGAS 집계 쿼리 파라미터.
 *
 * organizationId가 null이면 전체 기관을 대상으로 집계한다 (super_admin 전용).
 */
data class GetRagasEvaluationSummaryQuery(
    val organizationId: String?,
    val from: LocalDate,
    val to: LocalDate,
)

/**
 * 기간별 RAGAS 지표 집계를 반환하는 유스케이스.
 *
 * current 기간과 직전 동일 길이 기간을 함께 반환해 대시보드 Δ 계산에 사용한다.
 */
interface GetRagasEvaluationSummaryUseCase {
    fun getSummary(query: GetRagasEvaluationSummaryQuery): RagasEvaluationAggregate
}
