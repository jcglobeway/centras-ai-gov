package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.LoadLlmMetricsPort
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.LlmMetricsSummary
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * LLM 비용·토큰 집계 아웃바운드 어댑터.
 *
 * answers 테이블을 인메모리에서 집계한다.
 * 조직 스코프가 있으면 questions 테이블로 questionId를 먼저 조회한 뒤 필터링한다.
 */
open class LoadLlmMetricsPortAdapter(
    private val answerRepository: JpaAnswerRepository,
    private val questionRepository: JpaQuestionRepository,
) : LoadLlmMetricsPort {

    override fun loadMetrics(scope: ChatScope, from: String?, to: String?): LlmMetricsSummary {
        val fromInstant = from?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC) }
        val toInstant = to?.let { LocalDate.parse(it).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) }

        val answers = if (scope.globalAccess) {
            answerRepository.findAll()
        } else {
            val qIds = questionRepository.findAll()
                .filter { it.organizationId in scope.organizationIds }
                .map { it.id }
            if (qIds.isEmpty()) return empty()
            answerRepository.findByQuestionIdIn(qIds)
        }.filter { a ->
            (fromInstant == null || !a.createdAt.isBefore(fromInstant)) &&
            (toInstant == null || a.createdAt.isBefore(toInstant))
        }

        if (answers.isEmpty()) return empty()

        val withCost = answers.filter { it.estimatedCostUsd != null }
        val withTokens = answers.filter { it.inputTokens != null }

        return LlmMetricsSummary(
            answerCount = answers.size.toLong(),
            totalCostUsd = if (withCost.isEmpty()) null else withCost.sumOf { it.estimatedCostUsd!! },
            avgCostPerQuery = if (withCost.isEmpty()) null else withCost.map { it.estimatedCostUsd!! }.average(),
            avgInputTokens = if (withTokens.isEmpty()) null else withTokens.map { it.inputTokens!!.toDouble() }.average(),
            avgOutputTokens = if (withTokens.isEmpty()) null else withTokens.map { it.outputTokens!!.toDouble() }.average(),
        )
    }

    private fun empty() = LlmMetricsSummary(
        answerCount = 0, totalCostUsd = null,
        avgCostPerQuery = null, avgInputTokens = null, avgOutputTokens = null,
    )
}
