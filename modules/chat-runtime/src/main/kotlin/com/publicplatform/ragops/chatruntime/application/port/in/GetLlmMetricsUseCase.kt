package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.LlmMetricsSummary

/**
 * LLM 비용·토큰 집계 인바운드 포트.
 *
 * answers 테이블의 estimated_cost_usd, input_tokens, output_tokens 집계값을 반환한다.
 */
interface GetLlmMetricsUseCase {
    fun getMetrics(scope: ChatScope, from: String? = null, to: String? = null): LlmMetricsSummary
}
