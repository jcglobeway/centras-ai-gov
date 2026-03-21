package com.publicplatform.ragops.adminapi.evaluation.application.service

import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.ListRagasEvaluationsQuery
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.ListRagasEvaluationsUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.PagedResult
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.LoadRagasEvaluationsPort
import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationSummary

class ListRagasEvaluationsService(
    private val loadRagasEvaluationsPort: LoadRagasEvaluationsPort,
) : ListRagasEvaluationsUseCase {
    override fun listEvaluations(query: ListRagasEvaluationsQuery): PagedResult<RagasEvaluationSummary> {
        val items = loadRagasEvaluationsPort.loadAll(query.questionId, query.page, query.pageSize)
        val total = loadRagasEvaluationsPort.countAll(query.questionId)
        return PagedResult(items = items, total = total, page = query.page, pageSize = query.pageSize)
    }
}
