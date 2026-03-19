package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary

interface LoadQuestionPort {
    fun listQuestions(scope: ChatScope): List<QuestionSummary>
    fun listUnresolvedQuestions(scope: ChatScope): List<QuestionSummary>
}
