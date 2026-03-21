package com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence

import com.publicplatform.ragops.adminapi.evaluation.application.port.out.LoadRagasEvaluationsPort
import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationSummary
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

open class LoadRagasEvaluationsPortAdapter(
    private val jpaRepository: JpaRagasEvaluationRepository,
) : LoadRagasEvaluationsPort {

    override fun loadAll(questionId: String?, page: Int, pageSize: Int): List<RagasEvaluationSummary> {
        val pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "evaluatedAt"))
        return if (questionId != null) {
            jpaRepository.findByQuestionId(questionId, pageable).map { it.toSummary() }
        } else {
            jpaRepository.findAll(pageable).content.map { it.toSummary() }
        }
    }

    override fun countAll(questionId: String?): Long =
        if (questionId != null) jpaRepository.countByQuestionId(questionId)
        else jpaRepository.count()
}
