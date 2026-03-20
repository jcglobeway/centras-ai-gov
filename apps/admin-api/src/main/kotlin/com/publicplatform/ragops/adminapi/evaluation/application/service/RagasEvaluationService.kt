package com.publicplatform.ragops.adminapi.evaluation.application.service

import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.RecordRagasEvaluationCommand
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.RecordRagasEvaluationUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.SaveRagasEvaluationPort
import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationSummary
import java.time.LocalDateTime
import java.util.UUID

class RagasEvaluationService(
    private val saveRagasEvaluationPort: SaveRagasEvaluationPort,
) : RecordRagasEvaluationUseCase {

    override fun record(command: RecordRagasEvaluationCommand): RagasEvaluationSummary {
        val evaluation = RagasEvaluationSummary(
            id = "ragas_${UUID.randomUUID().toString().substring(0, 8)}",
            questionId = command.questionId,
            faithfulness = command.faithfulness,
            answerRelevancy = command.answerRelevancy,
            contextPrecision = command.contextPrecision,
            contextRecall = command.contextRecall,
            evaluatedAt = LocalDateTime.now(),
            judgeProvider = command.judgeProvider,
            judgeModel = command.judgeModel,
        )
        return saveRagasEvaluationPort.save(evaluation)
    }
}
