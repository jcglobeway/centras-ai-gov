/**
 * LoadMetricsPort의 JPA 구현체.
 *
 * 날짜 범위와 기관 범위로 필터링된 일별 KPI 지표를 반환한다.
 */
package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import com.publicplatform.ragops.metricsreporting.application.port.out.LoadMetricsPort
import java.time.LocalDate

open class LoadMetricsPortAdapter(
    private val jpaRepository: JpaDailyMetricsRepository,
) : LoadMetricsPort {

    override fun listDailyMetrics(
        scope: MetricsScope,
        fromDate: LocalDate?,
        toDate: LocalDate?,
    ): List<DailyMetricsSummary> {
        val startDate = fromDate ?: LocalDate.now().minusDays(30)
        val endDate = toDate ?: LocalDate.now()

        val allMetrics = jpaRepository.findByMetricDateBetweenOrderByMetricDateDesc(startDate, endDate)
            .map { it.toSummary() }

        return if (scope.globalAccess) allMetrics
        else allMetrics.filter { it.organizationId in scope.organizationIds }
    }
}
