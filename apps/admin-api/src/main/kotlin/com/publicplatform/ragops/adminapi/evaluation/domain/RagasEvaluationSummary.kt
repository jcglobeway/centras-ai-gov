package com.publicplatform.ragops.adminapi.evaluation.domain

import java.time.LocalDateTime

data class RagasEvaluationSummary(
    val id: String,
    val questionId: String,
    val organizationId: String?,
    val faithfulness: Double?,
    val answerRelevancy: Double?,
    val contextPrecision: Double?,
    val contextRecall: Double?,
    val citationCoverage: Double?,
    val citationCorrectness: Double?,
    val evaluatedAt: LocalDateTime,
    val judgeProvider: String?,
    val judgeModel: String?,
)
