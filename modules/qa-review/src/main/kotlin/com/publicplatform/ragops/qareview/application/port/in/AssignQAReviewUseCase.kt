package com.publicplatform.ragops.qareview.application.port.`in`

interface AssignQAReviewUseCase {
    fun execute(reviewId: String, assigneeId: String?)
}
