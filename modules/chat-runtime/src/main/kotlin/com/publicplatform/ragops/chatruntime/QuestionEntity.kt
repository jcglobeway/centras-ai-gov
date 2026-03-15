package com.publicplatform.ragops.chatruntime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "questions")
class QuestionEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "organization_id", nullable = false)
    val organizationId: String,

    @Column(name = "service_id", nullable = false)
    val serviceId: String,

    @Column(name = "chat_session_id", nullable = false)
    val chatSessionId: String,

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    val questionText: String,

    @Column(name = "question_intent_label")
    val questionIntentLabel: String?,

    @Column(name = "channel", nullable = false)
    val channel: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

fun QuestionEntity.toSummary(): QuestionSummary =
    QuestionSummary(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        chatSessionId = chatSessionId,
        questionText = questionText,
        questionIntentLabel = questionIntentLabel,
        channel = channel,
        createdAt = createdAt,
    )
