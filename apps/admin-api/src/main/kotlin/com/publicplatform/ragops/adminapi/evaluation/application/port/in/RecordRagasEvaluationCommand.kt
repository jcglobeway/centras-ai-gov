package com.publicplatform.ragops.adminapi.evaluation.application.port.`in`

data class RecordRagasEvaluationCommand(
    val questionId: String,
    val faithfulness: Double?,
    val answerRelevancy: Double?,
    val contextPrecision: Double?,
    val contextRecall: Double?,
    val judgeProvider: String?,
    val judgeModel: String?,
)
