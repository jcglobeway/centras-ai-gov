package com.publicplatform.ragops.adminapi.evaluation.application.service

import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.PatchRagasEvaluationCommand
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.PatchRagasEvaluationUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.PatchRagasEvaluationPort

class PatchRagasEvaluationService(
    private val patchRagasEvaluationPort: PatchRagasEvaluationPort,
) : PatchRagasEvaluationUseCase {
    override fun patch(command: PatchRagasEvaluationCommand): Boolean =
        patchRagasEvaluationPort.patch(
            questionId = command.questionId,
            faithfulness = command.faithfulness,
            answerRelevancy = command.answerRelevancy,
            contextPrecision = command.contextPrecision,
            contextRecall = command.contextRecall,
            citationCoverage = command.citationCoverage,
            citationCorrectness = command.citationCorrectness,
        )
}
