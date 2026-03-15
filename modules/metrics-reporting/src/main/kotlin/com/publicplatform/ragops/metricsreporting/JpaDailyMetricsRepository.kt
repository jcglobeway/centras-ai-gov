package com.publicplatform.ragops.metricsreporting

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface JpaDailyMetricsRepository : JpaRepository<DailyMetricsEntity, String> {
    fun findByMetricDateBetweenOrderByMetricDateDesc(startDate: LocalDate, endDate: LocalDate): List<DailyMetricsEntity>
}
