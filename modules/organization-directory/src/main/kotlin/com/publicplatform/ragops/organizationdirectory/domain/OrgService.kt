package com.publicplatform.ragops.organizationdirectory.domain

import java.time.Instant

data class Service(
    val id: String,
    val organizationId: String,
    val name: String,
    val channelType: String,
    val status: String,
    val goLiveAt: Instant?,
    val createdAt: Instant,
)
