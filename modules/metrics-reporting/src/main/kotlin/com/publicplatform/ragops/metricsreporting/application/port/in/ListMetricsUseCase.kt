package com.publicplatform.ragops.metricsreporting.application.port.`in`

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import java.time.LocalDate

/**
 * 일별 KPI 지표 목록 조회 인바운드 포트.
 *
 * 날짜 범위와 기관 범위로 필터링된 집계 지표를 반환한다.
 * 지표는 daily_metrics_org에 미리 집계된 스냅샷에서 조회하므로 실시간 집계 없음.
 */
interface ListMetricsUseCase {
    fun execute(scope: MetricsScope, fromDate: LocalDate?, toDate: LocalDate?): List<DailyMetricsSummary>
}
