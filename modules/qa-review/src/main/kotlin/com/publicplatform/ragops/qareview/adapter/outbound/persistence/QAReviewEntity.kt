/**
 * QAReview DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.qareview.adapter.outbound.persistence

import com.publicplatform.ragops.qareview.domain.ActionType
import com.publicplatform.ragops.qareview.domain.QAReviewSummary
import com.publicplatform.ragops.qareview.domain.QAReviewStatus
import com.publicplatform.ragops.qareview.domain.RootCauseCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "qa_reviews")
class QAReviewEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "question_id", nullable = false) val questionId: String,
    @Column(name = "review_status", nullable = false) val reviewStatus: String,
    @Column(name = "root_cause_code") val rootCauseCode: String?,
    @Column(name = "action_type") val actionType: String?,
    @Column(name = "action_target_id") val actionTargetId: String?,
    @Column(name = "review_comment", columnDefinition = "TEXT") val reviewComment: String?,
    @Column(name = "reviewer_id", nullable = false) val reviewerId: String,
    @Column(name = "assignee_id") val assigneeId: String?,
    @Column(name = "reviewed_at", nullable = false) val reviewedAt: Instant,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

fun QAReviewEntity.toSummary(): QAReviewSummary =
    QAReviewSummary(
        id = id, questionId = questionId, reviewStatus = reviewStatus.toReviewStatus(),
        rootCauseCode = rootCauseCode?.toRootCauseCode(), actionType = actionType?.toActionType(),
        actionTargetId = actionTargetId, reviewComment = reviewComment,
        reviewerId = reviewerId, assigneeId = assigneeId, reviewedAt = reviewedAt,
    )

fun QAReviewSummary.toEntity(): QAReviewEntity =
    QAReviewEntity(
        id = id, questionId = questionId, reviewStatus = reviewStatus.name.lowercase(),
        rootCauseCode = rootCauseCode?.name?.lowercase(), actionType = actionType?.name?.lowercase(),
        actionTargetId = actionTargetId, reviewComment = reviewComment,
        reviewerId = reviewerId, assigneeId = assigneeId, reviewedAt = reviewedAt, createdAt = Instant.now(),
    )

private fun String.toReviewStatus(): QAReviewStatus = when (this) {
    "pending" -> QAReviewStatus.PENDING; "confirmed_issue" -> QAReviewStatus.CONFIRMED_ISSUE
    "false_alarm" -> QAReviewStatus.FALSE_ALARM; "resolved" -> QAReviewStatus.RESOLVED
    else -> QAReviewStatus.PENDING
}

private fun String.toRootCauseCode(): RootCauseCode = when (this) {
    "missing_document" -> RootCauseCode.MISSING_DOCUMENT; "stale_document" -> RootCauseCode.STALE_DOCUMENT
    "bad_chunking" -> RootCauseCode.BAD_CHUNKING; "retrieval_failure" -> RootCauseCode.RETRIEVAL_FAILURE
    "generation_error" -> RootCauseCode.GENERATION_ERROR; "policy_block" -> RootCauseCode.POLICY_BLOCK
    "unclear_question" -> RootCauseCode.UNCLEAR_QUESTION; else -> RootCauseCode.UNCLEAR_QUESTION
}

private fun String.toActionType(): ActionType = when (this) {
    "faq_create" -> ActionType.FAQ_CREATE; "document_fix_request" -> ActionType.DOCUMENT_FIX_REQUEST
    "reindex_request" -> ActionType.REINDEX_REQUEST; "ops_issue" -> ActionType.OPS_ISSUE
    "no_action" -> ActionType.NO_ACTION; else -> ActionType.NO_ACTION
}
