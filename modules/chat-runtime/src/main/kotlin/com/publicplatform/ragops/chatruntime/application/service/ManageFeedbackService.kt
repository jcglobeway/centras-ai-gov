package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.ManageFeedbackUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadFeedbackPort
import com.publicplatform.ragops.chatruntime.application.port.out.RecordFeedbackPort
import com.publicplatform.ragops.chatruntime.domain.CreateFeedbackCommand
import com.publicplatform.ragops.chatruntime.domain.FeedbackScope
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary

/**
 * 시민 피드백 관리 유스케이스 구현체.
 *
 * 피드백 저장 및 조회를 RecordFeedbackPort/LoadFeedbackPort에 위임한다.
 */
open class ManageFeedbackService(
    private val feedbackWriter: RecordFeedbackPort,
    private val feedbackReader: LoadFeedbackPort,
) : ManageFeedbackUseCase {

    override fun create(command: CreateFeedbackCommand): FeedbackSummary =
        feedbackWriter.createFeedback(command)

    override fun list(scope: FeedbackScope): List<FeedbackSummary> =
        feedbackReader.listFeedbacks(scope)
}
