package com.publicplatform.ragops.metricsreporting.application.port.`in`

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.SaveDailyMetricsCommand

/**
 * 일별 KPI 지표 upsert 인바운드 포트.
 *
 * MetricsAggregationScheduler가 매일 새벽에 호출하여 전날 집계를 저장한다.
 * 동일 날짜·기관·서비스 조합이 이미 존재하면 덮어쓴다.
 */
interface UpsertDailyMetricsUseCase {
    fun execute(command: SaveDailyMetricsCommand): DailyMetricsSummary
}
