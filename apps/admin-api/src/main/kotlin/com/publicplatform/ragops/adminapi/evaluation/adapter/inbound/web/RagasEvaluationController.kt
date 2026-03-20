package com.publicplatform.ragops.adminapi.evaluation.adapter.inbound.web

import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.RecordRagasEvaluationCommand
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.RecordRagasEvaluationUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/ragas-evaluations")
class RagasEvaluationController(
    private val recordRagasEvaluationUseCase: RecordRagasEvaluationUseCase,
) {
    @PostMapping
    fun create(@RequestBody request: RecordRagasEvaluationRequest): ResponseEntity<RagasEvaluationResponse> {
        val result = recordRagasEvaluationUseCase.record(
            RecordRagasEvaluationCommand(
                questionId = request.questionId,
                faithfulness = request.faithfulness,
                answerRelevancy = request.answerRelevancy,
                contextPrecision = request.contextPrecision,
                contextRecall = request.contextRecall,
                judgeProvider = request.judgeProvider,
                judgeModel = request.judgeModel,
            )
        )
        return ResponseEntity.status(201).body(
            RagasEvaluationResponse(
                id = result.id,
                questionId = result.questionId,
                faithfulness = result.faithfulness,
                answerRelevancy = result.answerRelevancy,
                contextPrecision = result.contextPrecision,
                contextRecall = result.contextRecall,
                evaluatedAt = result.evaluatedAt.toString(),
                judgeProvider = result.judgeProvider,
                judgeModel = result.judgeModel,
            )
        )
    }
}

data class RecordRagasEvaluationRequest(
    val questionId: String,
    val faithfulness: Double? = null,
    val answerRelevancy: Double? = null,
    val contextPrecision: Double? = null,
    val contextRecall: Double? = null,
    val judgeProvider: String? = null,
    val judgeModel: String? = null,
)

data class RagasEvaluationResponse(
    val id: String,
    val questionId: String,
    val faithfulness: Double?,
    val answerRelevancy: Double?,
    val contextPrecision: Double?,
    val contextRecall: Double?,
    val evaluatedAt: String,
    val judgeProvider: String?,
    val judgeModel: String?,
)
