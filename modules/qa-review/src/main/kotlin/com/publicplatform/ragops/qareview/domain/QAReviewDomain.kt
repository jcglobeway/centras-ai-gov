/**
 * qa-review 바운디드 컨텍스트의 도메인 모델.
 *
 * QA 담당자가 미해결 질문을 검토하는 워크플로우를 표현한다.
 * QAReviewStateMachine은 false_alarm → resolved 전이 금지 등 도메인 규칙을 캡슐화한다.
 */
package com.publicplatform.ragops.qareview.domain

import java.time.Instant

enum class QAReviewStatus { PENDING, CONFIRMED_ISSUE, FALSE_ALARM, RESOLVED }
enum class RootCauseCode { MISSING_DOCUMENT, STALE_DOCUMENT, BAD_CHUNKING, RETRIEVAL_FAILURE, GENERATION_ERROR, POLICY_BLOCK, UNCLEAR_QUESTION }
enum class ActionType { FAQ_CREATE, DOCUMENT_FIX_REQUEST, REINDEX_REQUEST, OPS_ISSUE, NO_ACTION }

data class QAReviewSummary(
    val id: String,
    val questionId: String,
    val reviewStatus: QAReviewStatus,
    val rootCauseCode: RootCauseCode?,
    val actionType: ActionType?,
    val actionTargetId: String?,
    val reviewComment: String?,
    val reviewerId: String,
    val reviewedAt: Instant,
)

data class CreateQAReviewCommand(
    val questionId: String,
    val reviewStatus: QAReviewStatus,
    val rootCauseCode: RootCauseCode?,
    val actionType: ActionType?,
    val actionTargetId: String?,
    val reviewComment: String?,
    val reviewerId: String,
)

class InvalidQAReviewException(message: String) : RuntimeException(message)

object QAReviewStateMachine {
    fun validateReview(command: CreateQAReviewCommand) {
        when (command.reviewStatus) {
            QAReviewStatus.CONFIRMED_ISSUE -> {
                if (command.rootCauseCode == null) throw InvalidQAReviewException("confirmed_issue requires root_cause_code")
                if (command.actionType == null) throw InvalidQAReviewException("confirmed_issue requires action_type")
            }
            QAReviewStatus.FALSE_ALARM -> {
                if (command.actionType != null && command.actionType != ActionType.NO_ACTION) {
                    throw InvalidQAReviewException("false_alarm must have action_type = no_action or null")
                }
            }
            QAReviewStatus.RESOLVED -> {
                if (command.reviewComment.isNullOrBlank()) throw InvalidQAReviewException("resolved requires review_comment")
            }
            QAReviewStatus.PENDING -> {}
        }
    }

    fun validateTransition(previousStatus: QAReviewStatus?, nextStatus: QAReviewStatus) {
        if (previousStatus == QAReviewStatus.FALSE_ALARM && nextStatus == QAReviewStatus.RESOLVED) {
            throw InvalidQAReviewException("Cannot transition from false_alarm to resolved")
        }
        if (previousStatus == QAReviewStatus.RESOLVED && nextStatus == QAReviewStatus.FALSE_ALARM) {
            throw InvalidQAReviewException("Cannot transition from resolved to false_alarm")
        }
    }
}
