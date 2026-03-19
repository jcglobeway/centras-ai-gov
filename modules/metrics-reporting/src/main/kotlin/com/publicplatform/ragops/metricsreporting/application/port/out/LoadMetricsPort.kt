/**
 * 일별 KPI 지표 조회 아웃바운드 포트.
 *
 * MetricsScope와 날짜 범위로 필터링하며, fromDate/toDate가 null이면 전체 기간을 반환한다.
 */
package com.publicplatform.ragops.metricsreporting.application.port.out

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import java.time.LocalDate

interface LoadMetricsPort {
    fun listDailyMetrics(scope: MetricsScope, fromDate: LocalDate?, toDate: LocalDate?): List<DailyMetricsSummary>
}
