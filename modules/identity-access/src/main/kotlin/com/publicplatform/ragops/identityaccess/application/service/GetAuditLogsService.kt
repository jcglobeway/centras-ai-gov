package com.publicplatform.ragops.identityaccess.application.service

import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAuditLogsResult
import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAuditLogsUseCase
import com.publicplatform.ragops.identityaccess.application.port.out.LoadAuditLogPort

class GetAuditLogsService(
    private val loadAuditLogPort: LoadAuditLogPort,
) : GetAuditLogsUseCase {

    override fun list(page: Int, pageSize: Int): GetAuditLogsResult {
        val items = loadAuditLogPort.findRecent(page, pageSize)
        val total = loadAuditLogPort.count()
        return GetAuditLogsResult(items, total)
    }
}
