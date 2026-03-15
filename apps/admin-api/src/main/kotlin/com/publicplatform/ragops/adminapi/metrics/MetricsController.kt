package com.publicplatform.ragops.adminapi.metrics

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import com.publicplatform.ragops.metricsreporting.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@RestController
@RequestMapping("/admin")
class MetricsController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val metricsReader: MetricsReader,
) {

    @GetMapping("/metrics/daily")
    fun listDailyMetrics(
        @RequestParam(required = false) fromDate: String?,
        @RequestParam(required = false) toDate: String?,
        servletRequest: HttpServletRequest,
    ): DailyMetricsListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()

        val from = fromDate?.let { LocalDate.parse(it) }
        val to = toDate?.let { LocalDate.parse(it) }

        val metrics = metricsReader.listDailyMetrics(scope, from, to)

        return DailyMetricsListResponse(
            items = metrics.map { it.toResponse() },
            total = metrics.size,
        )
    }
}

data class DailyMetricsListResponse(
    val items: List<DailyMetricsResponse>,
    val total: Int,
)

data class DailyMetricsResponse(
    val id: String,
    val metricDate: String,
    val organizationId: String,
    val serviceId: String,
    val totalSessions: Int,
    val totalQuestions: Int,
    val resolvedRate: BigDecimal?,
    val fallbackRate: BigDecimal?,
    val zeroResultRate: BigDecimal?,
    val avgResponseTimeMs: Int?,
)

private fun DailyMetricsSummary.toResponse(): DailyMetricsResponse =
    DailyMetricsResponse(
        id = id,
        metricDate = metricDate.toString(),
        organizationId = organizationId,
        serviceId = serviceId,
        totalSessions = totalSessions,
        totalQuestions = totalQuestions,
        resolvedRate = resolvedRate,
        fallbackRate = fallbackRate,
        zeroResultRate = zeroResultRate,
        avgResponseTimeMs = avgResponseTimeMs,
    )

private fun AdminSessionSnapshot.toScope(): MetricsScope =
    MetricsScope(
        organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
        globalAccess = roleAssignments.any { it.organizationId == null },
    )
