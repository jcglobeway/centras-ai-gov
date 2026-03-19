package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.AnswerSummary

interface LoadAnswerPort {
    fun findByQuestionId(questionId: String): AnswerSummary?
}
