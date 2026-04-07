package com.publicplatform.ragops.chatruntime.domain

data class CreateCorrectionCommand(
    val organizationId: String,
    val serviceId: String,
    val questionId: String,
    val questionText: String,
    val originalAnswerText: String?,
    val correctedAnswerText: String,
    val correctedBy: String,
    val correctionReason: String?,
)
