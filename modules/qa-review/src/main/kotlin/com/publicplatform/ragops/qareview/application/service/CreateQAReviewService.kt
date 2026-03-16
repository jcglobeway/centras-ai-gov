package com.publicplatform.ragops.qareview.application.service

import com.publicplatform.ragops.qareview.application.port.`in`.CreateQAReviewUseCase
import com.publicplatform.ragops.qareview.application.port.out.RecordQAReviewPort
import com.publicplatform.ragops.qareview.domain.CreateQAReviewCommand
import com.publicplatform.ragops.qareview.domain.QAReviewSummary

/**
 * QA 리뷰 생성 유스케이스 구현체.
 *
 * RecordQAReviewPort에 위임하며, 상태 머신 유효성 검사는 어댑터 내에서 수행된다.
 */
open class CreateQAReviewService(
    private val qaReviewWriter: RecordQAReviewPort,
) : CreateQAReviewUseCase {

    override fun execute(command: CreateQAReviewCommand): QAReviewSummary =
        qaReviewWriter.createReview(command)
}
