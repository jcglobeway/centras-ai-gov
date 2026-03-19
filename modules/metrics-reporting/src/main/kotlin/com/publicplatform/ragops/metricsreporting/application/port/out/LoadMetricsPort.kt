package com.publicplatform.ragops.metricsreporting.application.port.out

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import java.time.LocalDate

interface LoadMetricsPort {
    fun listDailyMetrics(scope: MetricsScope, fromDate: LocalDate?, toDate: LocalDate?): List<DailyMetricsSummary>
}
