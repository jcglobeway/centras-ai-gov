package com.publicplatform.ragops.metricsreporting.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

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
    val autoResolutionRate: BigDecimal?,
    val escalationRate: BigDecimal?,
    val explicitResolutionRate: BigDecimal?,
    val estimatedResolutionRate: BigDecimal?,
    val revisitRate: BigDecimal?,
    val afterHoursRate: BigDecimal?,
    val avgSessionTurnCount: BigDecimal?,
    val knowledgeGapCount: Int,
    val unansweredCount: Int,
    val lowSatisfactionCount: Int,
    val createdAt: Instant,
)

data class MetricsScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)

data class SaveDailyMetricsCommand(
    val metricDate: LocalDate,
    val organizationId: String,
    val serviceId: String,
    val totalSessions: Int,
    val totalQuestions: Int,
    val fallbackRate: BigDecimal?,
    val zeroResultRate: BigDecimal?,
    val avgResponseTimeMs: Int?,
)
