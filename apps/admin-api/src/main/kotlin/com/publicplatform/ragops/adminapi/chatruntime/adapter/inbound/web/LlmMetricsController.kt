package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.chatruntime.application.port.`in`.GetLlmMetricsUseCase
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * LLM 비용·토큰 집계 HTTP 인바운드 어댑터.
 *
 * answers 테이블의 estimated_cost_usd, input/output_tokens 집계 결과를 반환한다.
 */
@RestController
@RequestMapping("/admin")
class LlmMetricsController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val getLlmMetricsUseCase: GetLlmMetricsUseCase,
) {

    @GetMapping("/metrics/llm")
    fun getLlmMetrics(
        httpRequest: HttpServletRequest,
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) fromDate: String?,
        @RequestParam("to_date", required = false) toDate: String?,
    ): LlmMetricsResponse {
        val session = adminRequestSessionResolver.resolve(httpRequest)
        val summary = getLlmMetricsUseCase.getMetrics(session.toScope(organizationId), fromDate, toDate)
        return LlmMetricsResponse(
            answerCount = summary.answerCount,
            totalCostUsd = summary.totalCostUsd,
            avgCostPerQuery = summary.avgCostPerQuery,
            avgInputTokens = summary.avgInputTokens,
            avgOutputTokens = summary.avgOutputTokens,
            generatedAt = Instant.now().toString(),
        )
    }
}

data class LlmMetricsResponse(
    val answerCount: Long,
    val totalCostUsd: Double?,
    val avgCostPerQuery: Double?,
    val avgInputTokens: Double?,
    val avgOutputTokens: Double?,
    val generatedAt: String,
)

private fun AdminSessionSnapshot.toScope(filterOrgId: String? = null): ChatScope {
    val globalAccess = roleAssignments.any { it.organizationId == null }
    val sessionOrgIds = roleAssignments.mapNotNull { it.organizationId }.toSet()
    return if (filterOrgId != null) {
        val allowed = globalAccess || filterOrgId in sessionOrgIds
        ChatScope(organizationIds = if (allowed) setOf(filterOrgId) else sessionOrgIds, globalAccess = false)
    } else {
        ChatScope(organizationIds = sessionOrgIds, globalAccess = globalAccess)
    }
}
