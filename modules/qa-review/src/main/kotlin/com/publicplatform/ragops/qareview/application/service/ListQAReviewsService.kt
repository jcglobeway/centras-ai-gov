package com.publicplatform.ragops.qareview.application.service

import com.publicplatform.ragops.qareview.application.port.`in`.ListQAReviewsUseCase
import com.publicplatform.ragops.qareview.application.port.out.LoadQAReviewPort
import com.publicplatform.ragops.qareview.domain.QAReviewSummary

/**
 * QA 리뷰 목록 조회 유스케이스 구현체.
 *
 * LoadQAReviewPort에 위임하여 리뷰 이력을 반환한다.
 */
open class ListQAReviewsService(
    private val qaReviewReader: LoadQAReviewPort,
) : ListQAReviewsUseCase {

    override fun listByQuestion(questionId: String): List<QAReviewSummary> =
        qaReviewReader.listReviews(questionId)

    override fun listAll(): List<QAReviewSummary> =
        qaReviewReader.listAllReviews()
}
