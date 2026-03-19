package com.publicplatform.ragops.chatruntime.domain

data class RagAnswerResult(
    val answerText: String,
    val answerStatus: String,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)
