/**
 * LoadQAReviewPort의 JPA 구현체.
 *
 * 특정 질문의 리뷰 이력 또는 전체 리뷰 목록을 반환한다.
 */
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

    override fun listByStatus(status: String): List<QAReviewSummary> =
        jpaRepository.findByReviewStatusOrderByReviewedAtDesc(status).map { it.toSummary() }
}
