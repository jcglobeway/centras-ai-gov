package com.publicplatform.ragops.identityaccess.domain

import java.time.Instant

data class AuditLogEntry(
    val id: String,
    val actorUserId: String?,
    val actorRoleCode: String?,
    val organizationId: String?,
    val actionCode: String,
    val resourceType: String?,
    val resourceId: String?,
    val requestId: String?,
    val traceId: String?,
    val resultCode: String,
    val createdAt: Instant,
)
