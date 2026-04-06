package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.application.port.`in`.AuditLogFilter
import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

interface LoadAuditLogPort {
    fun findByFilter(filter: AuditLogFilter, page: Int, pageSize: Int): List<AuditLogEntry>
    fun countByFilter(filter: AuditLogFilter): Long
    fun findAllByFilter(filter: AuditLogFilter): List<AuditLogEntry>
}
