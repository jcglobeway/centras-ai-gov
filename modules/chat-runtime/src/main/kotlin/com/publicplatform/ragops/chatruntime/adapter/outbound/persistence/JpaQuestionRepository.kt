/**
 * Question 관련 Spring Data JPA 레포지토리.
 *
 * Adapter 클래스에서만 사용하며, RepositoryConfiguration을 통해 주입된다.
 * Controller나 Service가 직접 참조하면 ArchUnit Rule 5가 실패한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface JpaQuestionRepository : JpaRepository<QuestionEntity, String> {
    @Modifying
    @Query("""
        UPDATE QuestionEntity q SET
          q.answerConfidence = :confidence,
          q.failureReasonCode = :failureCode,
          q.isEscalated = :isEscalated
        WHERE q.id = :questionId
    """)
    fun updateEnrichment(
        @Param("questionId") questionId: String,
        @Param("confidence") confidence: BigDecimal?,
        @Param("failureCode") failureCode: String?,
        @Param("isEscalated") isEscalated: Boolean,
    )

    @Query(value = """
        SELECT DISTINCT q.* FROM questions q
        LEFT JOIN answers a ON q.id = a.question_id
        LEFT JOIN qa_reviews qr ON q.id = qr.question_id
        WHERE a.answer_status IN ('fallback', 'no_answer', 'error')
        OR (
            qr.review_status = 'confirmed_issue'
            AND qr.reviewed_at = (
                SELECT MAX(qr2.reviewed_at) FROM qa_reviews qr2
                WHERE qr2.question_id = q.id
            )
        )
        ORDER BY q.created_at DESC
    """, nativeQuery = true)
    fun findUnresolvedQuestions(): List<QuestionEntity>
}
