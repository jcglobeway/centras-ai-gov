package com.publicplatform.ragops.identityaccess.application.service

import com.publicplatform.ragops.identityaccess.application.port.`in`.RecordAuditLogCommand
import com.publicplatform.ragops.identityaccess.application.port.`in`.RecordAuditLogUseCase
import com.publicplatform.ragops.identityaccess.application.port.out.RecordAuditLogPort
import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry
import java.time.Instant

class RecordAuditLogService(
    private val recordAuditLogPort: RecordAuditLogPort,
) : RecordAuditLogUseCase {

    override fun record(command: RecordAuditLogCommand): AuditLogEntry {
        val entry = AuditLogEntry(
            id = "",
            actionCode = command.actionCode,
            organizationId = command.organizationId,
            actorUserId = command.actorUserId,
            actorRoleCode = command.actorRoleCode,
            resourceType = command.resourceType,
            resourceId = command.resourceId,
            requestId = null,
            traceId = null,
            resultCode = command.resultCode,
            createdAt = Instant.now(),
        )
        return recordAuditLogPort.save(entry)
    }
}
