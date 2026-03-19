package com.publicplatform.ragops.metricsreporting.application.port.out

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.SaveDailyMetricsCommand

interface SaveMetricsPort {
    fun upsertDailyMetrics(command: SaveDailyMetricsCommand): DailyMetricsSummary
}
