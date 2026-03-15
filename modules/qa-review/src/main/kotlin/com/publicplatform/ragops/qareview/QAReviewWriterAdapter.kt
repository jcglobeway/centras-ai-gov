package com.publicplatform.ragops.qareview

import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class QAReviewWriterAdapter(
    private val jpaRepository: JpaQAReviewRepository,
) : QAReviewWriter {

    @Transactional
    override fun createReview(command: CreateQAReviewCommand): QAReviewSummary {
        // 검증 규칙 적용
        QAReviewStateMachine.validateReview(command)

        // 최신 리뷰 조회 (상태 전이 검증)
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

        // 상태 전이 검증
        QAReviewStateMachine.validateTransition(previousStatus, command.reviewStatus)

        // 엔티티 생성
        val id = "qa_rev_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = QAReviewEntity(
            id = id,
            questionId = command.questionId,
            reviewStatus = command.reviewStatus.name.lowercase(),
            rootCauseCode = command.rootCauseCode?.name?.lowercase(),
            actionType = command.actionType?.name?.lowercase(),
            actionTargetId = command.actionTargetId,
            reviewComment = command.reviewComment,
            reviewerId = command.reviewerId,
            reviewedAt = Instant.now(),
            createdAt = Instant.now(),
        )

        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
