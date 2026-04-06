/**
 * TriggerMetricsAggregationUseCase 구현체.
 *
 * 스케줄러의 aggregate() 메서드를 재사용하여 중복 로직 없이
 * 온디맨드 집계를 위임한다.
 */
package com.publicplatform.ragops.adminapi.metrics.application.service

import com.publicplatform.ragops.adminapi.metrics.application.port.`in`.TriggerMetricsAggregationUseCase
import com.publicplatform.ragops.adminapi.metrics.adapter.inbound.scheduler.MetricsAggregationScheduler
import java.time.LocalDate

class TriggerMetricsAggregationService(
    private val scheduler: MetricsAggregationScheduler,
) : TriggerMetricsAggregationUseCase {

    override fun trigger(date: LocalDate) {
        scheduler.aggregate(date)
    }
}
