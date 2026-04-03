package com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence

import com.publicplatform.ragops.adminapi.evaluation.application.port.out.PatchRagasEvaluationPort
import org.springframework.jdbc.core.JdbcTemplate

open class PatchRagasEvaluationPortAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : PatchRagasEvaluationPort {

    override fun patch(
        questionId: String,
        faithfulness: Double?,
        answerRelevancy: Double?,
        contextPrecision: Double?,
        contextRecall: Double?,
        citationCoverage: Double?,
        citationCorrectness: Double?,
    ): Boolean {
        val updated = jdbcTemplate.update(
            """
            UPDATE ragas_evaluations SET
              faithfulness         = COALESCE(?, faithfulness),
              answer_relevancy     = COALESCE(?, answer_relevancy),
              context_precision    = COALESCE(?, context_precision),
              context_recall       = COALESCE(?, context_recall),
              citation_coverage    = COALESCE(?, citation_coverage),
              citation_correctness = COALESCE(?, citation_correctness),
              evaluated_at         = NOW()
            WHERE question_id = ?
            """.trimIndent(),
            faithfulness,
            answerRelevancy,
            contextPrecision,
            contextRecall,
            citationCoverage,
            citationCorrectness,
            questionId,
        )
        return updated > 0
    }
}
