package com.publicplatform.ragops.chatruntime.domain

data class FaqCandidate(
    val questionId: String,
    val questionText: String,
    val questionCategory: String?,
    val similarId: String,
    val similarText: String,
    val similarity: Double,
)
