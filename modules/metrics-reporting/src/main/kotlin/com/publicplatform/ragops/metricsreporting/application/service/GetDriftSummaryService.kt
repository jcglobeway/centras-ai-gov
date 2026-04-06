package com.publicplatform.ragops.metricsreporting.application.service

import com.publicplatform.ragops.metricsreporting.application.port.`in`.GetDriftSummaryUseCase
import com.publicplatform.ragops.metricsreporting.application.port.out.LoadMetricsPort
import com.publicplatform.ragops.metricsreporting.domain.DriftSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

class GetDriftSummaryService(
    private val loadMetricsPort: LoadMetricsPort,
) : GetDriftSummaryUseCase {

    override fun getSummary(organizationIds: Set<String>, globalAccess: Boolean): List<DriftSummary> {
        val scope = MetricsScope(organizationIds = organizationIds, globalAccess = globalAccess)
        val to = LocalDate.now()
        val from = to.minusDays(14)
        val metrics = loadMetricsPort.listDailyMetrics(scope, from, to)
            .sortedBy { it.metricDate }

        if (metrics.isEmpty()) return emptyList()

        val recent14 = metrics.takeLast(14)
        val rolling7 = recent14.takeLast(7)
        val prev7 = recent14.take(7)

        return listOf(
            buildDrift("fallback_rate",
                prev7.mapNotNull { it.fallbackRate },
                rolling7.lastOrNull()?.fallbackRate
            ),
            buildDrift("zero_result_rate",
                prev7.mapNotNull { it.zeroResultRate },
                rolling7.lastOrNull()?.zeroResultRate
            ),
        )
    }

    private fun buildDrift(key: String, prevValues: List<BigDecimal>, latest: BigDecimal?): DriftSummary {
        val avg = if (prevValues.isEmpty()) null
        else prevValues.reduce { a, b -> a + b }.divide(BigDecimal(prevValues.size), MathContext.DECIMAL64)

        val deviation = if (avg != null && latest != null && avg > BigDecimal.ZERO) {
            (latest - avg).divide(avg, MathContext.DECIMAL64)
                .multiply(BigDecimal(100))
                .setScale(2, java.math.RoundingMode.HALF_UP)
        } else null

        return DriftSummary(metricKey = key, rollingAvg = avg?.setScale(2, java.math.RoundingMode.HALF_UP), latestValue = latest, deviationPct = deviation)
    }
}