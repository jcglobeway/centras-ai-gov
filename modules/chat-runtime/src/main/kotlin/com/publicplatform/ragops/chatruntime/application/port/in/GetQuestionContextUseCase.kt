package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.QuestionContextSummary

interface GetQuestionContextUseCase {
    fun getContext(questionId: String): QuestionContextSummary?
}
