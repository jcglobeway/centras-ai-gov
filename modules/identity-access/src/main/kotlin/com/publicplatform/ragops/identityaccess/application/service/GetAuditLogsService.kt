package com.publicplatform.ragops.identityaccess.application.service

import com.publicplatform.ragops.identityaccess.application.port.`in`.AuditLogFilter
import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAuditLogsResult
import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAuditLogsUseCase
import com.publicplatform.ragops.identityaccess.application.port.out.LoadAuditLogPort
import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

class GetAuditLogsService(
    private val loadAuditLogPort: LoadAuditLogPort,
) : GetAuditLogsUseCase {

    override fun list(filter: AuditLogFilter, page: Int, pageSize: Int): GetAuditLogsResult {
        val items = loadAuditLogPort.findByFilter(filter, page, pageSize)
        val total = loadAuditLogPort.countByFilter(filter)
        return GetAuditLogsResult(items, total)
    }

    override fun listAll(filter: AuditLogFilter): List<AuditLogEntry> =
        loadAuditLogPort.findAllByFilter(filter)
}
