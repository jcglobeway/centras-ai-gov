package com.publicplatform.ragops.chatruntime.domain

/**
 * LLM 비용·토큰 집계 결과 뷰.
 *
 * answers 테이블에서 estimated_cost_usd, input_tokens, output_tokens를 집계해 반환한다.
 * answerCount가 0이면 null 값이 반환될 수 있다.
 */
data class LlmMetricsSummary(
    val answerCount: Long,
    val totalCostUsd: Double?,
    val avgCostPerQuery: Double?,
    val avgInputTokens: Double?,
    val avgOutputTokens: Double?,
)
