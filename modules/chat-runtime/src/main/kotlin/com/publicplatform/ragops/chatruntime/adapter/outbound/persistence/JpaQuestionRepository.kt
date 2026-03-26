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
import java.time.Instant

interface UnresolvedRow {
    fun getQuestionId(): String
    fun getOrganizationId(): String
    fun getQuestionText(): String
    fun getFailureReasonCode(): String?
    fun getQuestionCategory(): String?
    fun getIsEscalated(): Boolean
    fun getAnswerStatus(): String?
    fun getLatestReviewStatus(): String?
    fun getCreatedAt(): Instant
}

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

    @Modifying
    @Query(
        value = "UPDATE questions SET question_embedding = CAST(:embedding AS vector) WHERE id = :questionId",
        nativeQuery = true,
    )
    fun updateEmbedding(
        @Param("questionId") questionId: String,
        @Param("embedding") embedding: String,
    )

    @Query(value = """
        SELECT DISTINCT q.id AS questionId,
               q.organization_id AS organizationId,
               q.question_text AS questionText,
               q.failure_reason_code AS failureReasonCode,
               q.question_category AS questionCategory,
               q.is_escalated AS isEscalated,
               q.created_at AS createdAt,
               a.answer_status AS answerStatus,
               (SELECT qr2.review_status FROM qa_reviews qr2
                WHERE qr2.question_id = q.id
                AND qr2.reviewed_at = (
                    SELECT MAX(qr3.reviewed_at) FROM qa_reviews qr3
                    WHERE qr3.question_id = q.id)
               ) AS latestReviewStatus
        FROM questions q
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
    fun findUnresolvedWithStatus(): List<UnresolvedRow>

    @Query(value = """
        SELECT q1.id AS questionId, q1.question_text AS questionText, q1.question_category AS questionCategory,
               q2.id AS similarId, q2.question_text AS similarText,
               1 - (q1.question_embedding <=> q2.question_embedding) AS similarity
        FROM questions q1
        JOIN questions q2 ON q1.id < q2.id
        WHERE q1.organization_id = :orgId
          AND q2.organization_id = :orgId
          AND q1.question_embedding IS NOT NULL
          AND q2.question_embedding IS NOT NULL
          AND 1 - (q1.question_embedding <=> q2.question_embedding) >= :threshold
        ORDER BY similarity DESC
        LIMIT 50
    """, nativeQuery = true)
    fun findFaqCandidates(
        @Param("orgId") orgId: String,
        @Param("threshold") threshold: Double,
    ): List<FaqCandidateProjection>
}
