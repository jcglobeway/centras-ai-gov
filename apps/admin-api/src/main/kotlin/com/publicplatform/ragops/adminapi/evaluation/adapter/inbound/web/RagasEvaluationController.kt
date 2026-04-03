package com.publicplatform.ragops.adminapi.evaluation.adapter.inbound.web

import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.GetRagasEvaluationSummaryQuery
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.GetRagasEvaluationSummaryUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.ListRagasEvaluationsQuery
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.ListRagasEvaluationsUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.PatchRagasEvaluationCommand
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.PatchRagasEvaluationUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.RecordRagasEvaluationCommand
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.RecordRagasEvaluationUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate

@RestController
@RequestMapping("/admin/ragas-evaluations")
class RagasEvaluationController(
    private val recordRagasEvaluationUseCase: RecordRagasEvaluationUseCase,
    private val listRagasEvaluationsUseCase: ListRagasEvaluationsUseCase,
    private val getRagasEvaluationSummaryUseCase: GetRagasEvaluationSummaryUseCase,
    private val patchRagasEvaluationUseCase: PatchRagasEvaluationUseCase,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) questionId: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(name = "page_size", defaultValue = "10") pageSize: Int,
    ): ResponseEntity<RagasEvaluationsPagedResponse> {
        val result = listRagasEvaluationsUseCase.listEvaluations(
            ListRagasEvaluationsQuery(questionId = questionId, page = page, pageSize = pageSize)
        )
        return ResponseEntity.ok(
            RagasEvaluationsPagedResponse(
                items = result.items.map {
                    RagasEvaluationResponse(
                        id = it.id,
                        questionId = it.questionId,
                        faithfulness = it.faithfulness,
                        answerRelevancy = it.answerRelevancy,
                        contextPrecision = it.contextPrecision,
                        contextRecall = it.contextRecall,
                        citationCoverage = it.citationCoverage,
                        citationCorrectness = it.citationCorrectness,
                        evaluatedAt = it.evaluatedAt.toString(),
                        judgeProvider = it.judgeProvider,
                        judgeModel = it.judgeModel,
                    )
                },
                total = result.total,
                page = result.page,
                pageSize = result.pageSize,
                generatedAt = Instant.now().toString(),
            )
        )
    }
    @GetMapping("/summary")
    fun summary(
        @RequestParam(name = "organization_id", required = false) organizationId: String?,
        @RequestParam(name = "from_date", required = false) fromDate: String?,
        @RequestParam(name = "to_date", required = false) toDate: String?,
    ): ResponseEntity<RagasEvaluationSummaryResponse> {
        // 기본 기간: 최근 7일 (to_date 기준 6일 전 ~ today)
        val to = if (toDate != null) LocalDate.parse(toDate) else LocalDate.now()
        val from = if (fromDate != null) LocalDate.parse(fromDate) else to.minusDays(6)
        val aggregate = getRagasEvaluationSummaryUseCase.getSummary(
            GetRagasEvaluationSummaryQuery(organizationId = organizationId, from = from, to = to)
        )
        return ResponseEntity.ok(
            RagasEvaluationSummaryResponse(
                current = RagasEvaluationPeriodResponse(
                    avgFaithfulness = aggregate.current.avgFaithfulness,
                    avgAnswerRelevancy = aggregate.current.avgAnswerRelevancy,
                    avgContextPrecision = aggregate.current.avgContextPrecision,
                    avgContextRecall = aggregate.current.avgContextRecall,
                    avgCitationCoverage = aggregate.current.avgCitationCoverage,
                    avgCitationCorrectness = aggregate.current.avgCitationCorrectness,
                    count = aggregate.current.count,
                    from = aggregate.current.from.toString(),
                    to = aggregate.current.to.toString(),
                ),
                previous = RagasEvaluationPeriodResponse(
                    avgFaithfulness = aggregate.previous.avgFaithfulness,
                    avgAnswerRelevancy = aggregate.previous.avgAnswerRelevancy,
                    avgContextPrecision = aggregate.previous.avgContextPrecision,
                    avgContextRecall = aggregate.previous.avgContextRecall,
                    avgCitationCoverage = aggregate.previous.avgCitationCoverage,
                    avgCitationCorrectness = aggregate.previous.avgCitationCorrectness,
                    count = aggregate.previous.count,
                    from = aggregate.previous.from.toString(),
                    to = aggregate.previous.to.toString(),
                ),
                generatedAt = Instant.now().toString(),
            )
        )
    }

    @GetMapping("/by-question/{questionId}")
    fun getByQuestion(@PathVariable questionId: String): ResponseEntity<RagasEvaluationResponse> {
        val result = listRagasEvaluationsUseCase.listEvaluations(
            ListRagasEvaluationsQuery(questionId = questionId, page = 1, pageSize = 1)
        )
        val item = result.items.firstOrNull()
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            RagasEvaluationResponse(
                id = item.id,
                questionId = item.questionId,
                faithfulness = item.faithfulness,
                answerRelevancy = item.answerRelevancy,
                contextPrecision = item.contextPrecision,
                contextRecall = item.contextRecall,
                citationCoverage = item.citationCoverage,
                citationCorrectness = item.citationCorrectness,
                evaluatedAt = item.evaluatedAt.toString(),
                judgeProvider = item.judgeProvider,
                judgeModel = item.judgeModel,
            )
        )
    }

    @PatchMapping("/by-question/{questionId}")
    fun patchByQuestion(
        @PathVariable questionId: String,
        @RequestBody request: PatchRagasEvaluationRequest,
    ): ResponseEntity<Void> {
        val updated = patchRagasEvaluationUseCase.patch(
            PatchRagasEvaluationCommand(
                questionId = questionId,
                faithfulness = request.faithfulness,
                answerRelevancy = request.answerRelevancy,
                contextPrecision = request.contextPrecision,
                contextRecall = request.contextRecall,
                citationCoverage = request.citationCoverage,
                citationCorrectness = request.citationCorrectness,
            )
        )
        return if (updated) ResponseEntity.ok().build() else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun create(@RequestBody request: RecordRagasEvaluationRequest): ResponseEntity<RagasEvaluationResponse> {
        val result = recordRagasEvaluationUseCase.record(
            RecordRagasEvaluationCommand(
                questionId = request.questionId,
                organizationId = request.organizationId,
                faithfulness = request.faithfulness,
                answerRelevancy = request.answerRelevancy,
                contextPrecision = request.contextPrecision,
                contextRecall = request.contextRecall,
                citationCoverage = request.citationCoverage,
                citationCorrectness = request.citationCorrectness,
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
                citationCoverage = result.citationCoverage,
                citationCorrectness = result.citationCorrectness,
                evaluatedAt = result.evaluatedAt.toString(),
                judgeProvider = result.judgeProvider,
                judgeModel = result.judgeModel,
            )
        )
    }
}

data class RecordRagasEvaluationRequest(
    val questionId: String,
    val organizationId: String? = null,
    val faithfulness: Double? = null,
    val answerRelevancy: Double? = null,
    val contextPrecision: Double? = null,
    val contextRecall: Double? = null,
    val citationCoverage: Double? = null,
    val citationCorrectness: Double? = null,
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
    val citationCoverage: Double?,
    val citationCorrectness: Double?,
    val evaluatedAt: String,
    val judgeProvider: String?,
    val judgeModel: String?,
)

data class RagasEvaluationsPagedResponse(
    val items: List<RagasEvaluationResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val generatedAt: String,
)

data class RagasEvaluationPeriodResponse(
    val avgFaithfulness: Double?,
    val avgAnswerRelevancy: Double?,
    val avgContextPrecision: Double?,
    val avgContextRecall: Double?,
    val avgCitationCoverage: Double?,
    val avgCitationCorrectness: Double?,
    val count: Long,
    val from: String,
    val to: String,
)

data class RagasEvaluationSummaryResponse(
    val current: RagasEvaluationPeriodResponse,
    val previous: RagasEvaluationPeriodResponse,
    val generatedAt: String,
)

data class PatchRagasEvaluationRequest(
    val faithfulness: Double? = null,
    val answerRelevancy: Double? = null,
    val contextPrecision: Double? = null,
    val contextRecall: Double? = null,
    val citationCoverage: Double? = null,
    val citationCorrectness: Double? = null,
)
