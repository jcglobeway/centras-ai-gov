/**
 * QA 리뷰 생성 아웃바운드 포트.
 *
 * QA 리뷰는 append-only이므로 update 없이 새 레코드만 생성한다.
 * 상태 전이 유효성은 QAReviewStateMachine에서 사전 검증된다.
 */
package com.publicplatform.ragops.qareview.application.port.out

import com.publicplatform.ragops.qareview.domain.CreateQAReviewCommand
import com.publicplatform.ragops.qareview.domain.QAReviewSummary

interface RecordQAReviewPort {
    fun createReview(command: CreateQAReviewCommand): QAReviewSummary
}
