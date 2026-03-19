package com.publicplatform.ragops.ingestionops.domain

data class IngestionScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
