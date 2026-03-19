package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class ChatSessionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val channel: String,
    val userKeyHash: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val sessionEndType: String?,
    val totalQuestionCount: Int,
)
