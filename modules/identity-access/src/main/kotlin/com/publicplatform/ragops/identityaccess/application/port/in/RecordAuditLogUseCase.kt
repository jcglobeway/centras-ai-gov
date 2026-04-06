package com.publicplatform.ragops.identityaccess.application.port.`in`

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

interface RecordAuditLogUseCase {
    fun record(command: RecordAuditLogCommand): AuditLogEntry
}

data class RecordAuditLogCommand(
    val actionCode: String,
    val organizationId: String? = null,
    val actorUserId: String? = null,
    val actorRoleCode: String? = null,
    val resourceType: String? = null,
    val resourceId: String? = null,
    val resultCode: String = "success",
)
