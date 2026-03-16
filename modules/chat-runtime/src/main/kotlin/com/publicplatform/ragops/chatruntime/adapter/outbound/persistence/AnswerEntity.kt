package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.AnswerStatus
import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "answers")
class AnswerEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "question_id", nullable = false) val questionId: String,
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT") val answerText: String,
    @Column(name = "answer_status", nullable = false) val answerStatus: String,
    @Column(name = "response_time_ms") val responseTimeMs: Int?,
    @Column(name = "citation_count") val citationCount: Int?,
    @Column(name = "fallback_reason_code") val fallbackReasonCode: String?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

fun AnswerEntity.toSummary(): AnswerSummary =
    AnswerSummary(
        id = id, questionId = questionId, answerText = answerText,
        answerStatus = answerStatus.toAnswerStatus(), responseTimeMs = responseTimeMs,
        citationCount = citationCount, fallbackReasonCode = fallbackReasonCode, createdAt = createdAt,
    )

private fun String.toAnswerStatus(): AnswerStatus = when (this) {
    "answered" -> AnswerStatus.ANSWERED; "fallback" -> AnswerStatus.FALLBACK
    "no_answer" -> AnswerStatus.NO_ANSWER; else -> AnswerStatus.ERROR
}
