package com.publicplatform.ragops.adminapi.metrics.adapter.inbound.event

import com.publicplatform.ragops.adminapi.metrics.adapter.inbound.scheduler.MetricsAggregationScheduler
import com.publicplatform.ragops.qareview.domain.QAReviewResolvedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase
import java.time.LocalDate

@Component
class QAReviewResolvedEventHandler(
    private val metricsAggregationScheduler: MetricsAggregationScheduler,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: QAReviewResolvedEvent) {
        metricsAggregationScheduler.aggregate(LocalDate.now())
    }
}
