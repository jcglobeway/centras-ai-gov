package com.publicplatform.ragops.metricsreporting

import java.math.BigDecimal
import java.time.LocalDate
import java.time.Instant

data class DailyMetricsSummary(
    val id: String,
    val metricDate: LocalDate,
    val organizationId: String,
    val serviceId: String,
    val totalSessions: Int,
    val totalQuestions: Int,
    val resolvedRate: BigDecimal?,
    val fallbackRate: BigDecimal?,
    val zeroResultRate: BigDecimal?,
    val avgResponseTimeMs: Int?,
    val createdAt: Instant,
)

data class MetricsScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)

interface MetricsReader {
    fun listDailyMetrics(scope: MetricsScope, fromDate: LocalDate?, toDate: LocalDate?): List<DailyMetricsSummary>
}
