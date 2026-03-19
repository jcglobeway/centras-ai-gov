package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class AnswerSummary(
    val id: String,
    val questionId: String,
    val answerText: String,
    val answerStatus: AnswerStatus,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
    val createdAt: Instant,
)

data class CreateAnswerCommand(
    val questionId: String,
    val answerText: String,
    val answerStatus: AnswerStatus,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)
