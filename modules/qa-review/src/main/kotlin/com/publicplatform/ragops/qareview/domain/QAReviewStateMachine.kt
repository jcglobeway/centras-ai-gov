package com.publicplatform.ragops.qareview.domain

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
