package com.publicplatform.ragops.adminapi.evaluation.application.port.`in`

data class PatchRagasEvaluationCommand(
    val questionId: String,
    val faithfulness: Double? = null,
    val answerRelevancy: Double? = null,
    val contextPrecision: Double? = null,
    val contextRecall: Double? = null,
    val citationCoverage: Double? = null,
    val citationCorrectness: Double? = null,
)

interface PatchRagasEvaluationUseCase {
    fun patch(command: PatchRagasEvaluationCommand): Boolean
}
