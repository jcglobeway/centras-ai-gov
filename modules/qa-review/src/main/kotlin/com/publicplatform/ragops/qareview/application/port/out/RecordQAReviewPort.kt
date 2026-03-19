package com.publicplatform.ragops.qareview.application.port.out

import com.publicplatform.ragops.qareview.domain.CreateQAReviewCommand
import com.publicplatform.ragops.qareview.domain.QAReviewSummary

interface RecordQAReviewPort {
    fun createReview(command: CreateQAReviewCommand): QAReviewSummary
}
