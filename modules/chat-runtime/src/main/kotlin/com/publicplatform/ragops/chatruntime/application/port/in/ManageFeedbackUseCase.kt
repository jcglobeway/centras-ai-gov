package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.CreateFeedbackCommand
import com.publicplatform.ragops.chatruntime.domain.FeedbackScope
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary

/**
 * 시민 피드백 관리 인바운드 포트.
 *
 * 피드백 생성 및 목록 조회를 담당한다.
 */
interface ManageFeedbackUseCase {
    fun create(command: CreateFeedbackCommand): FeedbackSummary
    fun list(scope: FeedbackScope): List<FeedbackSummary>
}
