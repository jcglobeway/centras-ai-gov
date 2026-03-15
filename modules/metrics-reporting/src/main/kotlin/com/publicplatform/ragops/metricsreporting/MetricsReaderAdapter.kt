package com.publicplatform.ragops.metricsreporting

import java.time.LocalDate

open class MetricsReaderAdapter(
    private val jpaRepository: JpaDailyMetricsRepository,
) : MetricsReader {

    override fun listDailyMetrics(
        scope: MetricsScope,
        fromDate: LocalDate?,
        toDate: LocalDate?,
    ): List<DailyMetricsSummary> {
        val startDate = fromDate ?: LocalDate.now().minusDays(30)
        val endDate = toDate ?: LocalDate.now()

        val allMetrics = jpaRepository.findByMetricDateBetweenOrderByMetricDateDesc(startDate, endDate)
            .map { it.toSummary() }

        return if (scope.globalAccess) {
            allMetrics
        } else {
            allMetrics.filter { it.organizationId in scope.organizationIds }
        }
    }
}
