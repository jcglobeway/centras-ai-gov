package com.publicplatform.ragops.qareview.application.port.`in`

import com.publicplatform.ragops.qareview.domain.CreateQAReviewCommand
import com.publicplatform.ragops.qareview.domain.QAReviewSummary

/**
 * QA 리뷰 생성 인바운드 포트.
 *
 * QA 담당자가 미해결 질문에 대한 검토 결과를 기록할 때 호출된다.
 * 상태 머신 유효성 검사는 QAReviewStateMachine에 위임한다.
 */
interface CreateQAReviewUseCase {
    fun execute(command: CreateQAReviewCommand): QAReviewSummary
}
