package com.publicplatform.ragops.qareview

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaQAReviewRepository : JpaRepository<QAReviewEntity, String> {
    fun findByQuestionIdOrderByReviewedAtDesc(questionId: String): List<QAReviewEntity>
}
