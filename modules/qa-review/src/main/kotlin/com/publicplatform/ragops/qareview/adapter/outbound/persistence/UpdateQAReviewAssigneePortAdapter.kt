package com.publicplatform.ragops.qareview.adapter.outbound.persistence

import com.publicplatform.ragops.qareview.application.port.out.UpdateQAReviewAssigneePort
import org.springframework.transaction.annotation.Transactional

open class UpdateQAReviewAssigneePortAdapter(
    private val jpaRepository: JpaQAReviewRepository,
) : UpdateQAReviewAssigneePort {

    @Transactional
    override fun updateAssignee(reviewId: String, assigneeId: String?) {
        jpaRepository.updateAssigneeId(reviewId, assigneeId)
    }
}
