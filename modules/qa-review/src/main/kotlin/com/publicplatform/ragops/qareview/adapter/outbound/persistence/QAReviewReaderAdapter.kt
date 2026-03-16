package com.publicplatform.ragops.qareview.adapter.outbound.persistence

import com.publicplatform.ragops.qareview.domain.QAReviewSummary
import com.publicplatform.ragops.qareview.application.port.out.LoadQAReviewPort

open class LoadQAReviewPortAdapter(
    private val jpaRepository: JpaQAReviewRepository,
) : LoadQAReviewPort {

    override fun listReviews(questionId: String): List<QAReviewSummary> =
        jpaRepository.findByQuestionIdOrderByReviewedAtDesc(questionId).map { it.toSummary() }

    override fun listAllReviews(): List<QAReviewSummary> =
        jpaRepository.findAll().map { it.toSummary() }
}
