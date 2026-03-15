package com.publicplatform.ragops.qareview

object QAReviewStateMachine {
    /**
     * QA Review 생성 시 검증 규칙을 적용한다.
     */
    fun validateReview(command: CreateQAReviewCommand) {
        when (command.reviewStatus) {
            QAReviewStatus.CONFIRMED_ISSUE -> {
                if (command.rootCauseCode == null) {
                    throw InvalidQAReviewException("confirmed_issue requires root_cause_code")
                }
                if (command.actionType == null) {
                    throw InvalidQAReviewException("confirmed_issue requires action_type")
                }
            }

            QAReviewStatus.FALSE_ALARM -> {
                if (command.actionType != null && command.actionType != ActionType.NO_ACTION) {
                    throw InvalidQAReviewException("false_alarm must have action_type = no_action or null")
                }
            }

            QAReviewStatus.RESOLVED -> {
                if (command.reviewComment.isNullOrBlank()) {
                    throw InvalidQAReviewException("resolved requires review_comment")
                }
            }

            QAReviewStatus.PENDING -> {
                // pending은 자유롭게 저장 가능
            }
        }
    }

    /**
     * 상태 전이가 허용되는지 검증한다.
     * 현재는 append-only 모델이므로 이전 상태 확인만 수행.
     */
    fun validateTransition(
        previousStatus: QAReviewStatus?,
        nextStatus: QAReviewStatus,
    ) {
        // 금지 전이: false_alarm → resolved
        if (previousStatus == QAReviewStatus.FALSE_ALARM && nextStatus == QAReviewStatus.RESOLVED) {
            throw InvalidQAReviewException("Cannot transition from false_alarm to resolved")
        }

        // 금지 전이: resolved → false_alarm
        if (previousStatus == QAReviewStatus.RESOLVED && nextStatus == QAReviewStatus.FALSE_ALARM) {
            throw InvalidQAReviewException("Cannot transition from resolved to false_alarm")
        }

        // 나머지 전이는 허용 (pending ↔ confirmed_issue ↔ resolved)
    }
}
