package com.publicplatform.ragops.qareview.domain

import com.publicplatform.ragops.sharedkernel.DomainEvent
import java.time.Instant

data class QAReviewResolvedEvent(
    val reviewId: String,
    val questionId: String,
    val finalStatus: QAReviewStatus,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
