package com.publicplatform.ragops.metricsreporting.application.service

import com.publicplatform.ragops.metricsreporting.application.port.`in`.ListMetricsUseCase
import com.publicplatform.ragops.metricsreporting.application.port.out.LoadMetricsPort
import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import java.time.LocalDate

/**
 * 일별 KPI 지표 목록 조회 유스케이스 구현체.
 *
 * LoadMetricsPort에 위임하여 날짜·기관 범위로 필터링된 지표를 반환한다.
 */
open class ListMetricsService(
    private val metricsReader: LoadMetricsPort,
) : ListMetricsUseCase {

    override fun execute(scope: MetricsScope, fromDate: LocalDate?, toDate: LocalDate?): List<DailyMetricsSummary> =
        metricsReader.listDailyMetrics(scope, fromDate, toDate)
}
