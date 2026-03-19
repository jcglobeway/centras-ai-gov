package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class FeedbackSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val questionId: String?,
    val sessionId: String?,
    val rating: Int,
    val comment: String?,
    val channel: String?,
    val feedbackType: String?,
    val clickedLink: Boolean,
    val clickedDocument: Boolean,
    val targetActionType: String?,
    val targetActionCompleted: Boolean,
    val dwellTimeMs: Long?,
    val submittedAt: Instant,
)

data class CreateFeedbackCommand(
    val organizationId: String,
    val serviceId: String,
    val questionId: String?,
    val sessionId: String?,
    val rating: Int,
    val comment: String?,
    val channel: String?,
    val feedbackType: String? = null,
    val clickedLink: Boolean = false,
    val clickedDocument: Boolean = false,
    val targetActionType: String? = null,
    val targetActionCompleted: Boolean = false,
    val dwellTimeMs: Long? = null,
)

data class FeedbackScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
