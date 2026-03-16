package com.publicplatform.ragops.metricsreporting.application.service

import com.publicplatform.ragops.metricsreporting.application.port.`in`.UpsertDailyMetricsUseCase
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveMetricsPort
import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.SaveDailyMetricsCommand

/**
 * 일별 KPI 지표 upsert 유스케이스 구현체.
 *
 * SaveMetricsPort에 위임하여 집계 결과를 저장한다.
 */
open class UpsertDailyMetricsService(
    private val metricsWriter: SaveMetricsPort,
) : UpsertDailyMetricsUseCase {

    override fun execute(command: SaveDailyMetricsCommand): DailyMetricsSummary =
        metricsWriter.upsertDailyMetrics(command)
}
