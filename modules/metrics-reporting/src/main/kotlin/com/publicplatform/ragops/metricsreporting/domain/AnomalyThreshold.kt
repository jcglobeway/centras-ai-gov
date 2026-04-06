package com.publicplatform.ragops.metricsreporting.domain

import java.math.BigDecimal
import java.time.Instant

data class AnomalyThreshold(
    val metricKey: String,
    val warnValue: BigDecimal,
    val criticalValue: BigDecimal,
    val updatedAt: Instant,
)

data class AlertEvent(
    val id: String,
    val metricKey: String,
    val currentValue: BigDecimal,
    val severity: String,
    val triggeredAt: Instant,
)

data class DriftSummary(
    val metricKey: String,
    val rollingAvg: BigDecimal?,
    val latestValue: BigDecimal?,
    val deviationPct: BigDecimal?,
)

data class UpdateThresholdCommand(
    val metricKey: String,
    val warnValue: BigDecimal,
    val criticalValue: BigDecimal,
)
