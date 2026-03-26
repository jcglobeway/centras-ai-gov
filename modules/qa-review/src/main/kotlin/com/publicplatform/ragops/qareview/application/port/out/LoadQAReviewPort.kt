/**
 * QA 리뷰 목록 조회 아웃바운드 포트.
 *
 * listReviews()는 특정 질문의 리뷰 이력을 반환하고,
 * listAllReviews()는 전체 리뷰를 반환한다(관리자 대시보드 전용).
 */
package com.publicplatform.ragops.qareview.application.port.out

import com.publicplatform.ragops.qareview.domain.QAReviewSummary

interface LoadQAReviewPort {
    fun listReviews(questionId: String): List<QAReviewSummary>
    fun listAllReviews(): List<QAReviewSummary>
    fun listByStatus(status: String): List<QAReviewSummary>
}
