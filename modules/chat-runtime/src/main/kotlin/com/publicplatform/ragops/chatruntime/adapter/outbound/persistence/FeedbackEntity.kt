package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "feedbacks")
class FeedbackEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "organization_id", nullable = false) val organizationId: String,
    @Column(name = "service_id", nullable = false) val serviceId: String,
    @Column(name = "question_id") val questionId: String?,
    @Column(name = "session_id") val sessionId: String?,
    @Column(name = "rating", nullable = false) val rating: Int,
    @Column(name = "comment", columnDefinition = "TEXT") val comment: String?,
    @Column(name = "channel") val channel: String?,
    @Column(name = "submitted_at", nullable = false) val submittedAt: Instant = Instant.now(),
)

fun FeedbackEntity.toSummary(): FeedbackSummary =
    FeedbackSummary(
        id = id, organizationId = organizationId, serviceId = serviceId,
        questionId = questionId, sessionId = sessionId, rating = rating,
        comment = comment, channel = channel, submittedAt = submittedAt,
    )
