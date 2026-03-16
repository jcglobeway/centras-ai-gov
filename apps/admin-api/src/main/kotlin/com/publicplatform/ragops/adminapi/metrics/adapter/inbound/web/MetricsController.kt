package com.publicplatform.ragops.adminapi.metrics.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.metricsreporting.application.port.`in`.ListMetricsUseCase
import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지표 대시보드 HTTP 인바운드 어댑터.
 *
 * 일별 KPI 지표 목록 조회를 ListMetricsUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class MetricsController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val listMetricsUseCase: ListMetricsUseCase,
) {

    @GetMapping("/metrics/daily")
    fun listDailyMetrics(
        @RequestParam(required = false) fromDate: String?,
        @RequestParam(required = false) toDate: String?,
        servletRequest: HttpServletRequest,
    ): DailyMetricsListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)

        val metrics = listMetricsUseCase.execute(
            scope = session.toScope(),
            fromDate = fromDate?.let { LocalDate.parse(it) },
            toDate = toDate?.let { LocalDate.parse(it) },
        )

        return DailyMetricsListResponse(items = metrics.map { it.toResponse() }, total = metrics.size)
    }
}

data class DailyMetricsListResponse(val items: List<DailyMetricsResponse>, val total: Int)

data class DailyMetricsResponse(
    val id: String, val metricDate: String, val organizationId: String, val serviceId: String,
    val totalSessions: Int, val totalQuestions: Int, val resolvedRate: BigDecimal?,
    val fallbackRate: BigDecimal?, val zeroResultRate: BigDecimal?, val avgResponseTimeMs: Int?,
)

private fun DailyMetricsSummary.toResponse() = DailyMetricsResponse(
    id = id, metricDate = metricDate.toString(), organizationId = organizationId,
    serviceId = serviceId, totalSessions = totalSessions, totalQuestions = totalQuestions,
    resolvedRate = resolvedRate, fallbackRate = fallbackRate, zeroResultRate = zeroResultRate,
    avgResponseTimeMs = avgResponseTimeMs,
)

private fun AdminSessionSnapshot.toScope() = MetricsScope(
    organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
    globalAccess = roleAssignments.any { it.organizationId == null },
)
