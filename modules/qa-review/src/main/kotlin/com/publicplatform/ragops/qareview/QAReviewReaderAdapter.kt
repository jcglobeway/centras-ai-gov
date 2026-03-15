package com.publicplatform.ragops.qareview

open class QAReviewReaderAdapter(
    private val jpaRepository: JpaQAReviewRepository,
) : QAReviewReader {

    override fun listReviews(questionId: String): List<QAReviewSummary> {
        return jpaRepository.findByQuestionIdOrderByReviewedAtDesc(questionId)
            .map { it.toSummary() }
    }

    override fun listAllReviews(): List<QAReviewSummary> {
        return jpaRepository.findAll().map { it.toSummary() }
    }
}
