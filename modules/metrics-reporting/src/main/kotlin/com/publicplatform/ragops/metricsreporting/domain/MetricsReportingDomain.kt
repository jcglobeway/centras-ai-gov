/**
 * metrics-reporting 바운디드 컨텍스트의 도메인 모델.
 *
 * 기관·서비스별 일별 KPI 지표 스냅샷을 표현한다.
 * 지표는 항상 사전 집계된 값이며 온디맨드 집계를 수행하지 않는다.
 */
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
