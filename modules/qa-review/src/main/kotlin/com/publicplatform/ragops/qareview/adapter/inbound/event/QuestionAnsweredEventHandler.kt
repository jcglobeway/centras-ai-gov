package com.publicplatform.ragops.qareview.adapter.inbound.event

import com.publicplatform.ragops.chatruntime.domain.AnswerStatus
import com.publicplatform.ragops.chatruntime.domain.QuestionAnsweredEvent
import com.publicplatform.ragops.qareview.application.port.`in`.CreateQAReviewUseCase
import com.publicplatform.ragops.qareview.domain.CreateQAReviewCommand
import com.publicplatform.ragops.qareview.domain.QAReviewStatus
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase

open class QuestionAnsweredEventHandler(
    private val createQAReviewUseCase: CreateQAReviewUseCase,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: QuestionAnsweredEvent) {
        if (event.answerStatus !in setOf(AnswerStatus.FALLBACK, AnswerStatus.NO_ANSWER, AnswerStatus.ERROR)) return

        createQAReviewUseCase.execute(
            CreateQAReviewCommand(
                questionId = event.questionId,
                reviewStatus = QAReviewStatus.PENDING,
                rootCauseCode = null,
                actionType = null,
                actionTargetId = null,
                reviewComment = null,
                reviewerId = "system",
            ),
        )
    }
}
