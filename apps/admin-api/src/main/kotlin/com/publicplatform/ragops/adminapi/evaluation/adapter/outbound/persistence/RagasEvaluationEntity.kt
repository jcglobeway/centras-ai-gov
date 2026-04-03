package com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence

import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ragas_evaluations")
class RagasEvaluationEntity(
    @Id val id: String,
    @Column(name = "question_id", nullable = false) val questionId: String,
    @Column(name = "organization_id") val organizationId: String?,
    @Column val faithfulness: Double?,
    @Column(name = "answer_relevancy") val answerRelevancy: Double?,
    @Column(name = "context_precision") val contextPrecision: Double?,
    @Column(name = "context_recall") val contextRecall: Double?,
    @Column(name = "citation_coverage") val citationCoverage: Double?,
    @Column(name = "citation_correctness") val citationCorrectness: Double?,
    @Column(name = "evaluated_at", nullable = false) val evaluatedAt: LocalDateTime,
    @Column(name = "judge_provider") val judgeProvider: String?,
    @Column(name = "judge_model") val judgeModel: String?,
) {
    fun toSummary() = RagasEvaluationSummary(
        id = id, questionId = questionId, organizationId = organizationId,
        faithfulness = faithfulness, answerRelevancy = answerRelevancy,
        contextPrecision = contextPrecision, contextRecall = contextRecall,
        citationCoverage = citationCoverage, citationCorrectness = citationCorrectness,
        evaluatedAt = evaluatedAt, judgeProvider = judgeProvider, judgeModel = judgeModel,
    )
}

fun RagasEvaluationSummary.toEntity() = RagasEvaluationEntity(
    id = id, questionId = questionId, organizationId = organizationId,
    faithfulness = faithfulness, answerRelevancy = answerRelevancy,
    contextPrecision = contextPrecision, contextRecall = contextRecall,
    citationCoverage = citationCoverage, citationCorrectness = citationCorrectness,
    evaluatedAt = evaluatedAt, judgeProvider = judgeProvider, judgeModel = judgeModel,
)
