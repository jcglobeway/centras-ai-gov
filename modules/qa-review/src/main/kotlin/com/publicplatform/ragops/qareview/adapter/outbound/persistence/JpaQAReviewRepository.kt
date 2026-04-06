/**
 * QAReview 관련 Spring Data JPA 레포지토리.
 *
 * Adapter 클래스에서만 사용하며, RepositoryConfiguration을 통해 주입된다.
 * Controller나 Service가 직접 참조하면 ArchUnit Rule 5가 실패한다.
 */
package com.publicplatform.ragops.qareview.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface JpaQAReviewRepository : JpaRepository<QAReviewEntity, String> {
    fun findByQuestionIdOrderByReviewedAtDesc(questionId: String): List<QAReviewEntity>
    fun findByReviewStatusOrderByReviewedAtDesc(reviewStatus: String): List<QAReviewEntity>

    @Modifying
    @Query(value = "UPDATE qa_reviews SET assignee_id = :assigneeId WHERE id = :id", nativeQuery = true)
    fun updateAssigneeId(@Param("id") id: String, @Param("assigneeId") assigneeId: String?)
}
