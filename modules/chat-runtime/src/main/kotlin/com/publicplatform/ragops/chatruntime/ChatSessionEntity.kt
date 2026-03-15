package com.publicplatform.ragops.chatruntime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "organization_id", nullable = false)
    val organizationId: String,

    @Column(name = "service_id", nullable = false)
    val serviceId: String,

    @Column(name = "channel", nullable = false)
    val channel: String,

    @Column(name = "user_key_hash")
    val userKeyHash: String?,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,

    @Column(name = "ended_at")
    val endedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

fun ChatSessionEntity.toSummary(): ChatSessionSummary =
    ChatSessionSummary(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        channel = channel,
        userKeyHash = userKeyHash,
        startedAt = startedAt,
        endedAt = endedAt,
    )
