package com.publicplatform.ragops.identityaccess.application.port.`in`

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

interface GetAuditLogsUseCase {
    fun list(filter: AuditLogFilter, page: Int, pageSize: Int): GetAuditLogsResult
    fun listAll(filter: AuditLogFilter): List<AuditLogEntry>
}

data class GetAuditLogsResult(
    val items: List<AuditLogEntry>,
    val total: Long,
)
