package com.publicplatform.ragops.identityaccess.application.port.`in`

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

interface GetAuditLogsUseCase {
    fun list(page: Int, pageSize: Int): GetAuditLogsResult
}

data class GetAuditLogsResult(
    val items: List<AuditLogEntry>,
    val total: Long,
)
