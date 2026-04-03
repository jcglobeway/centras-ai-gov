package com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence

import com.publicplatform.ragops.adminapi.evaluation.application.port.out.LoadRagasEvaluationSummaryPort
import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationPeriodSummary
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

/**
 * JDBC native SQL로 ragas_evaluations 테이블을 기간·기관 단위로 집계하는 어댑터.
 *
 * JPA를 사용하지 않는 이유: AVG 집계 쿼리는 엔티티 매핑이 불필요하고
 * organization_id 필터가 null 여부에 따라 파라미터 수가 달라져 native SQL이 더 간결하다.
 */
open class LoadRagasEvaluationSummaryPortAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : LoadRagasEvaluationSummaryPort {

    override fun loadSummary(
        organizationId: String?,
        from: LocalDate,
        to: LocalDate,
    ): RagasEvaluationPeriodSummary {
        val orgFilter = if (organizationId != null) "AND organization_id = ?" else ""
        val sql = "SELECT AVG(faithfulness) AS avg_faithfulness, AVG(answer_relevancy) AS avg_answer_relevancy, AVG(context_precision) AS avg_context_precision, AVG(context_recall) AS avg_context_recall, AVG(citation_coverage) AS avg_citation_coverage, AVG(citation_correctness) AS avg_citation_correctness, COUNT(*) AS cnt FROM ragas_evaluations WHERE evaluated_at >= ? AND evaluated_at < ? $orgFilter"

        val params: Array<Any> = if (organizationId != null) {
            arrayOf(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), organizationId)
        } else {
            arrayOf(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
        }

        return jdbcTemplate.queryForObject(sql, params) { rs, _ ->
            RagasEvaluationPeriodSummary(
                avgFaithfulness = rs.getObject("avg_faithfulness") as? Double,
                avgAnswerRelevancy = rs.getObject("avg_answer_relevancy") as? Double,
                avgContextPrecision = rs.getObject("avg_context_precision") as? Double,
                avgContextRecall = rs.getObject("avg_context_recall") as? Double,
                avgCitationCoverage = rs.getObject("avg_citation_coverage") as? Double,
                avgCitationCorrectness = rs.getObject("avg_citation_correctness") as? Double,
                count = rs.getLong("cnt"),
                from = from,
                to = to,
            )
        } ?: RagasEvaluationPeriodSummary(
            avgFaithfulness = null,
            avgAnswerRelevancy = null,
            avgContextPrecision = null,
            avgContextRecall = null,
            avgCitationCoverage = null,
            avgCitationCorrectness = null,
            count = 0L,
            from = from,
            to = to,
        )
    }
}
