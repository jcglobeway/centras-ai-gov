package com.publicplatform.ragops.sharedkernel

import java.time.Instant

interface DomainEvent {
    val occurredAt: Instant
}

