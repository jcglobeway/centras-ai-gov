package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.LoadFaqCandidatesPort
import com.publicplatform.ragops.chatruntime.domain.FaqCandidate

open class LoadFaqCandidatesPortAdapter(
    private val jpaRepository: JpaQuestionRepository,
) : LoadFaqCandidatesPort {

    override fun findFaqCandidates(organizationId: String, threshold: Double): List<FaqCandidate> =
        jpaRepository.findFaqCandidates(organizationId, threshold).map { row ->
            FaqCandidate(
                questionId = row.getQuestionId(),
                questionText = row.getQuestionText(),
                questionCategory = row.getQuestionCategory(),
                similarId = row.getSimilarId(),
                similarText = row.getSimilarText(),
                similarity = row.getSimilarity(),
            )
        }
}
