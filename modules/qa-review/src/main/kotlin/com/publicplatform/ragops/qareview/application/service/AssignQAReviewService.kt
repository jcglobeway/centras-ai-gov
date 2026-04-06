package com.publicplatform.ragops.qareview.application.service

import com.publicplatform.ragops.qareview.application.port.`in`.AssignQAReviewUseCase
import com.publicplatform.ragops.qareview.application.port.out.UpdateQAReviewAssigneePort

open class AssignQAReviewService(
    private val updateQAReviewAssigneePort: UpdateQAReviewAssigneePort,
) : AssignQAReviewUseCase {

    override fun execute(reviewId: String, assigneeId: String?) {
        updateQAReviewAssigneePort.updateAssignee(reviewId, assigneeId)
    }
}
