/**
 * 기관·서비스별 일별 KPI 지표 스냅샷 도메인 모델.
 *
 * 지표는 항상 사전 집계된 값이며 온디맨드 집계를 수행하지 않는다.
 * MetricsAggregationScheduler가 매일 00:05에 전날 데이터를 upsert한다.
 * SaveDailyMetricsCommand는 집계 결과를 저장할 때 사용하는 입력 커맨드이다.
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
    val resolvedRate: BigDecimal? = null,
    val fallbackRate: BigDecimal?,
    val zeroResultRate: BigDecimal?,
    val avgResponseTimeMs: Int?,
    val autoResolutionRate: BigDecimal? = null,
    val escalationRate: BigDecimal? = null,
    val explicitResolutionRate: BigDecimal? = null,
    val estimatedResolutionRate: BigDecimal? = null,
    val revisitRate: BigDecimal? = null,
    val afterHoursRate: BigDecimal? = null,
    val avgSessionTurnCount: BigDecimal? = null,
    val knowledgeGapCount: Int = 0,
    val unansweredCount: Int = 0,
    val lowSatisfactionCount: Int = 0,
)
