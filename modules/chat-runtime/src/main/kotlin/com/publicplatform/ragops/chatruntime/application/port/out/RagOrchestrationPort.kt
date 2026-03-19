package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.RagAnswerResult

interface RagOrchestrationPort {
    fun generateAnswer(
        questionId: String,
        questionText: String,
        organizationId: String,
        serviceId: String,
    ): RagAnswerResult?
}
