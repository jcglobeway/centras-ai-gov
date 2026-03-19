package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.CreateFeedbackCommand
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary

interface RecordFeedbackPort {
    fun createFeedback(command: CreateFeedbackCommand): FeedbackSummary
}
