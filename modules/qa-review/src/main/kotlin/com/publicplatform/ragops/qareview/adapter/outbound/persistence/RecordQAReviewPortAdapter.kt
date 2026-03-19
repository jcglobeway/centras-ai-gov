/**
 * RecordQAReviewPort의 JPA 구현체.
 *
 * 리뷰 생성 전 QAReviewStateMachine으로 유효성을 검사한다.
 * 리뷰는 append-only이므로 UPDATE 없이 INSERT만 수행한다.
 */
package com.publicplatform.ragops.qareview.adapter.outbound.persistence

import com.publicplatform.ragops.qareview.domain.CreateQAReviewCommand
import com.publicplatform.ragops.qareview.domain.QAReviewStateMachine
import com.publicplatform.ragops.qareview.domain.QAReviewStatus
import com.publicplatform.ragops.qareview.domain.QAReviewSummary
import com.publicplatform.ragops.qareview.application.port.out.RecordQAReviewPort
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class RecordQAReviewPortAdapter(
    private val jpaRepository: JpaQAReviewRepository,
) : RecordQAReviewPort {

    @Transactional
    override fun createReview(command: CreateQAReviewCommand): QAReviewSummary {
        QAReviewStateMachine.validateReview(command)

        val previousReviews = jpaRepository.findByQuestionIdOrderByReviewedAtDesc(command.questionId)
        val previousStatus = previousReviews.firstOrNull()?.reviewStatus?.let {
            when (it) {
                "pending" -> QAReviewStatus.PENDING
                "confirmed_issue" -> QAReviewStatus.CONFIRMED_ISSUE
                "false_alarm" -> QAReviewStatus.FALSE_ALARM
                "resolved" -> QAReviewStatus.RESOLVED
                else -> null
            }
        }

        QAReviewStateMachine.validateTransition(previousStatus, command.reviewStatus)

        val id = "qa_rev_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = QAReviewEntity(
            id = id, questionId = command.questionId,
            reviewStatus = command.reviewStatus.name.lowercase(),
            rootCauseCode = command.rootCauseCode?.name?.lowercase(),
            actionType = command.actionType?.name?.lowercase(),
            actionTargetId = command.actionTargetId, reviewComment = command.reviewComment,
            reviewerId = command.reviewerId, reviewedAt = Instant.now(), createdAt = Instant.now(),
        )

        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
