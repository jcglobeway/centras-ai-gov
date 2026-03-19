package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import com.publicplatform.ragops.chatruntime.domain.CreateAnswerCommand

interface RecordAnswerPort {
    fun createAnswer(command: CreateAnswerCommand): AnswerSummary
}
