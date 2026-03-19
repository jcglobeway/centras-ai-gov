package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.CreateQuestionCommand
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary

interface RecordQuestionPort {
    fun createQuestion(command: CreateQuestionCommand): QuestionSummary
}
