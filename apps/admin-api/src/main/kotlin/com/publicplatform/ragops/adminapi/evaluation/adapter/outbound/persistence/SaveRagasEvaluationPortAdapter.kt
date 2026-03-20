package com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence

import com.publicplatform.ragops.adminapi.evaluation.application.port.out.SaveRagasEvaluationPort
import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationSummary

open class SaveRagasEvaluationPortAdapter(
    private val jpaRepository: JpaRagasEvaluationRepository,
) : SaveRagasEvaluationPort {
    override fun save(evaluation: RagasEvaluationSummary): RagasEvaluationSummary =
        jpaRepository.save(evaluation.toEntity()).toSummary()
}
