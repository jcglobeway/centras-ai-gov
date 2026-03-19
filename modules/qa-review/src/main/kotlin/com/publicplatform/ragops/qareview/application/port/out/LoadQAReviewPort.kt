package com.publicplatform.ragops.qareview.application.port.out

import com.publicplatform.ragops.qareview.domain.QAReviewSummary

interface LoadQAReviewPort {
    fun listReviews(questionId: String): List<QAReviewSummary>
    fun listAllReviews(): List<QAReviewSummary>
}
