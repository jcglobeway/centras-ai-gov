package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

interface RecordAuditLogPort {
    fun save(entry: AuditLogEntry): AuditLogEntry
}
