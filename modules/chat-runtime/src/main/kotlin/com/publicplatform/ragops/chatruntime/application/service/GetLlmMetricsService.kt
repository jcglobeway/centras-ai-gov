package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.GetLlmMetricsUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadLlmMetricsPort
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.LlmMetricsSummary

open class GetLlmMetricsService(
    private val loadLlmMetricsPort: LoadLlmMetricsPort,
) : GetLlmMetricsUseCase {

    override fun getMetrics(scope: ChatScope, from: String?, to: String?): LlmMetricsSummary =
        loadLlmMetricsPort.loadMetrics(scope, from, to)
}
