package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

interface LoadAuditLogPort {
    fun findRecent(page: Int, pageSize: Int): List<AuditLogEntry>
    fun count(): Long
}
