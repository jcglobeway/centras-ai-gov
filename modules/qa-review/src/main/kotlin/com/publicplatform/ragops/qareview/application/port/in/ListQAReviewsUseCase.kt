package com.publicplatform.ragops.qareview.application.port.`in`

import com.publicplatform.ragops.qareview.domain.QAReviewSummary

/**
 * QA 리뷰 목록 조회 인바운드 포트.
 *
 * 특정 질문의 리뷰 이력 또는 전체 리뷰 목록을 반환한다.
 */
interface ListQAReviewsUseCase {
    fun listByQuestion(questionId: String): List<QAReviewSummary>
    fun listAll(): List<QAReviewSummary>
    fun listByStatus(status: String): List<QAReviewSummary>
}
