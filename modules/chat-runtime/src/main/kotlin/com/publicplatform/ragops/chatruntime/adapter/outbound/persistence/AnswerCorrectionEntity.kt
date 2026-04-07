package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.AnswerCorrectionSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "answer_corrections")
class AnswerCorrectionEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "organization_id", nullable = false) val organizationId: String,
    @Column(name = "service_id", nullable = false) val serviceId: String,
    @Column(name = "question_id", nullable = false) val questionId: String,
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT") val questionText: String,
    @Column(name = "original_answer_text", columnDefinition = "TEXT") val originalAnswerText: String?,
    @Column(name = "corrected_answer_text", nullable = false, columnDefinition = "TEXT") val correctedAnswerText: String,
    @Column(name = "corrected_by", nullable = false) val correctedBy: String,
    @Column(name = "correction_reason", columnDefinition = "TEXT") val correctionReason: String?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

fun AnswerCorrectionEntity.toSummary(): AnswerCorrectionSummary =
    AnswerCorrectionSummary(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        questionId = questionId,
        questionText = questionText,
        originalAnswerText = originalAnswerText,
        correctedAnswerText = correctedAnswerText,
        correctedBy = correctedBy,
        correctionReason = correctionReason,
        createdAt = createdAt,
    )
