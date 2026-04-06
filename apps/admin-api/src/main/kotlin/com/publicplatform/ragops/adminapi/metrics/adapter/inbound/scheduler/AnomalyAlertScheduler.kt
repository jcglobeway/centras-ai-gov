package com.publicplatform.ragops.adminapi.metrics.adapter.inbound.scheduler

import com.publicplatform.ragops.metricsreporting.application.port.out.LoadAnomalyThresholdPort
import com.publicplatform.ragops.metricsreporting.application.port.out.LoadMetricsPort
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveAlertEventPort
import com.publicplatform.ragops.metricsreporting.domain.AlertEvent
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Component
class AnomalyAlertScheduler(
    private val loadMetricsPort: LoadMetricsPort,
    private val loadAnomalyThresholdPort: LoadAnomalyThresholdPort,
    private val saveAlertEventPort: SaveAlertEventPort,
) {

    @Scheduled(fixedRate = 3_600_000)
    fun checkThresholds() {
        val thresholds = loadAnomalyThresholdPort.findAll().associateBy { it.metricKey }
        if (thresholds.isEmpty()) return

        val scope = MetricsScope(organizationIds = emptySet(), globalAccess = true)
        val today = LocalDate.now()
        val metrics = loadMetricsPort.listDailyMetrics(scope, today.minusDays(1), today)
        val latest = metrics.maxByOrNull { it.metricDate } ?: return

        data class Check(val key: String, val value: BigDecimal?)

        val checks = listOf(
            Check("fallback_rate", latest.fallbackRate),
            Check("zero_result_rate", latest.zeroResultRate),
            Check("avg_response_time_ms", latest.avgResponseTimeMs?.let { BigDecimal(it) }),
        )

        for (check in checks) {
            val value = check.value ?: continue
            val threshold = thresholds[check.key] ?: continue

            val severity = when {
                value >= threshold.criticalValue -> "critical"
                value >= threshold.warnValue -> "warn"
                else -> continue
            }

            saveAlertEventPort.save(
                AlertEvent(
                    id = "alert_${UUID.randomUUID().toString().substring(0, 8)}",
                    metricKey = check.key,
                    currentValue = value,
                    severity = severity,
                    triggeredAt = java.time.Instant.now(),
                )
            )
        }
    }
}
