package com.publicplatform.ragops.chatruntime

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JpaQuestionRepository : JpaRepository<QuestionEntity, String> {
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
