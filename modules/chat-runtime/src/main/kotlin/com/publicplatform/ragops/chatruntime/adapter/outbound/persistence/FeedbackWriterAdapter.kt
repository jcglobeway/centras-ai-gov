/**
 * RecordFeedbackPort의 JPA 구현체.
 *
 * 시민 피드백(별점, 코멘트)을 feedbacks 테이블에 저장한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.CreateFeedbackCommand
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary
import com.publicplatform.ragops.chatruntime.application.port.out.RecordFeedbackPort
import java.util.UUID

open class RecordFeedbackPortAdapter(
    private val jpaRepository: JpaFeedbackRepository,
) : RecordFeedbackPort {

    override fun createFeedback(command: CreateFeedbackCommand): FeedbackSummary {
        val entity = FeedbackEntity(
            id = "feedback_${UUID.randomUUID().toString().substring(0, 8)}",
            organizationId = command.organizationId, serviceId = command.serviceId,
            questionId = command.questionId, sessionId = command.sessionId,
            rating = command.rating, comment = command.comment, channel = command.channel,
        )
        return jpaRepository.save(entity).toSummary()
    }
}
