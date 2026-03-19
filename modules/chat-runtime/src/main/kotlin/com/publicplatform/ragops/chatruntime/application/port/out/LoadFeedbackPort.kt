package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.FeedbackScope
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary

interface LoadFeedbackPort {
    fun listFeedbacks(scope: FeedbackScope): List<FeedbackSummary>
}
