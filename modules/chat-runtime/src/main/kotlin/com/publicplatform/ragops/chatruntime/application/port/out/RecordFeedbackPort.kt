/**
 * 시민 피드백 생성 아웃바운드 포트.
 *
 * 시민이 답변에 평점 또는 행동 신호를 남기면 이 포트를 통해 feedbacks 테이블에 저장한다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.CreateFeedbackCommand
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary

interface RecordFeedbackPort {
    fun createFeedback(command: CreateFeedbackCommand): FeedbackSummary
}
