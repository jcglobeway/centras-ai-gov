package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class AnswerCorrectionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val questionId: String,
    val questionText: String,
    val originalAnswerText: String?,
    val correctedAnswerText: String,
    val correctedBy: String,
    val correctionReason: String?,
    val createdAt: Instant,
)

data class CorrectionScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
