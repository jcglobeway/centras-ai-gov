package com.publicplatform.ragops.adminapi.evaluation.application.service

import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.GetRagasEvaluationSummaryQuery
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.GetRagasEvaluationSummaryUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.LoadRagasEvaluationSummaryPort
import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationAggregate

class GetRagasEvaluationSummaryService(
    private val loadRagasEvaluationSummaryPort: LoadRagasEvaluationSummaryPort,
) : GetRagasEvaluationSummaryUseCase {

    override fun getSummary(query: GetRagasEvaluationSummaryQuery): RagasEvaluationAggregate {
        val current = loadRagasEvaluationSummaryPort.loadSummary(
            organizationId = query.organizationId,
            from = query.from,
            to = query.to,
        )

        // 이전 기간 = current 기간과 동일한 길이만큼 바로 앞 구간
        // ex) current = 3/26~4/2(8일) → previous = 3/18~3/25(8일)
        val periodDays = query.to.toEpochDay() - query.from.toEpochDay() + 1
        val prevTo = query.from.minusDays(1)
        val prevFrom = prevTo.minusDays(periodDays - 1)

        val previous = loadRagasEvaluationSummaryPort.loadSummary(
            organizationId = query.organizationId,
            from = prevFrom,
            to = prevTo,
        )

        return RagasEvaluationAggregate(current = current, previous = previous)
    }
}
