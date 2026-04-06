package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.GetQuestionContextUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadRagSearchLogPort
import com.publicplatform.ragops.chatruntime.domain.QuestionContextSummary

open class GetQuestionContextService(
    private val loadRagSearchLogPort: LoadRagSearchLogPort,
) : GetQuestionContextUseCase {

    override fun getContext(questionId: String): QuestionContextSummary? =
        loadRagSearchLogPort.getQuestionContext(questionId)
}
