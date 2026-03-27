package com.publicplatform.ragops.adminapi.metrics.adapter.inbound.event

import com.publicplatform.ragops.adminapi.metrics.adapter.inbound.scheduler.MetricsAggregationScheduler
import com.publicplatform.ragops.ingestionops.domain.IngestionJobCompletedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase
import java.time.LocalDate

@Component
class IngestionJobCompletedEventHandler(
    private val metricsAggregationScheduler: MetricsAggregationScheduler,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: IngestionJobCompletedEvent) {
        metricsAggregationScheduler.aggregate(LocalDate.now())
    }
}
