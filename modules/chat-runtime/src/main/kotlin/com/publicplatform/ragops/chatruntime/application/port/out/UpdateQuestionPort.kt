package com.publicplatform.ragops.chatruntime.application.port.out

import java.math.BigDecimal

interface UpdateQuestionPort {
    fun updateAfterAnswer(
        questionId: String,
        confidenceScore: BigDecimal?,
        failureReasonCode: String?,
        isEscalated: Boolean,
    )
    fun updateEmbedding(questionId: String, embedding: String)
}
