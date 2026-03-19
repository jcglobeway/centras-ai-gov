package com.publicplatform.ragops.organizationdirectory.domain

import java.time.Instant

data class OrganizationSummary(
    val id: String,
    val name: String,
    val institutionType: String,
)

data class Organization(
    val id: String,
    val name: String,
    val orgCode: String,
    val status: String,
    val institutionType: String,
    val ownerUserId: String?,
    val lastDocumentSyncAt: Instant?,
    val createdAt: Instant,
)

data class OrganizationScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
