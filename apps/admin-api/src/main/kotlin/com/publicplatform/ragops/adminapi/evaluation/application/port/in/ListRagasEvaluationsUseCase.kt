package com.publicplatform.ragops.adminapi.evaluation.application.port.`in`

import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationSummary

data class ListRagasEvaluationsQuery(
    val questionId: String? = null,
    val page: Int = 1,
    val pageSize: Int = 10,
)

data class PagedResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

interface ListRagasEvaluationsUseCase {
    fun listEvaluations(query: ListRagasEvaluationsQuery): PagedResult<RagasEvaluationSummary>
}
