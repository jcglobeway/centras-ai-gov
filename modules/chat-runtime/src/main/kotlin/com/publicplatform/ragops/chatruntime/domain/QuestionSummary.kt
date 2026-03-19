package com.publicplatform.ragops.chatruntime.domain

import java.math.BigDecimal
import java.time.Instant

data class QuestionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val questionCategory: String?,
    val answerConfidence: BigDecimal?,
    val failureReasonCode: String?,
    val isEscalated: Boolean,
    val createdAt: Instant,
)

data class CreateQuestionCommand(
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val failureReasonCode: String? = null,
)
