package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.UpdateQuestionPort
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

open class UpdateQuestionPortAdapter(
    private val jpaRepository: JpaQuestionRepository,
) : UpdateQuestionPort {

    @Transactional
    override fun updateAfterAnswer(
        questionId: String,
        confidenceScore: BigDecimal?,
        failureReasonCode: String?,
        isEscalated: Boolean,
    ) {
        jpaRepository.updateEnrichment(questionId, confidenceScore, failureReasonCode, isEscalated)
    }

    @Transactional
    override fun updateEmbedding(questionId: String, embedding: String) {
        jpaRepository.updateEmbedding(questionId, embedding)
    }
}
