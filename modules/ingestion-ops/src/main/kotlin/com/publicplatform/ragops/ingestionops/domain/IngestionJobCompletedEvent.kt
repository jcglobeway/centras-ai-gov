package com.publicplatform.ragops.ingestionops.domain

import com.publicplatform.ragops.sharedkernel.DomainEvent
import java.time.Instant

data class IngestionJobCompletedEvent(
    val jobId: String,
    val organizationId: String,
    val serviceId: String,
    val success: Boolean,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
