package com.publicplatform.ragops.adminapi.auth.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAuditLogsUseCase
import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin")
class AuditLogController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val getAuditLogsUseCase: GetAuditLogsUseCase,
) {

    @GetMapping("/audit-logs")
    fun listAuditLogs(
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("page_size", defaultValue = "20") pageSize: Int,
        servletRequest: HttpServletRequest,
    ): AuditLogListResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val result = getAuditLogsUseCase.list(page, pageSize)
        return AuditLogListResponse(
            items = result.items.map { it.toResponse() },
            total = result.total.toInt(),
        )
    }
}

data class AuditLogListResponse(val items: List<AuditLogResponse>, val total: Int)

data class AuditLogResponse(
    val id: String,
    val actorUserId: String?,
    val actorRoleCode: String?,
    val organizationId: String?,
    val actionCode: String,
    val resourceType: String?,
    val resourceId: String?,
    val resultCode: String,
    val createdAt: Instant,
)

private fun AuditLogEntry.toResponse() = AuditLogResponse(
    id = id,
    actorUserId = actorUserId,
    actorRoleCode = actorRoleCode,
    organizationId = organizationId,
    actionCode = actionCode,
    resourceType = resourceType,
    resourceId = resourceId,
    resultCode = resultCode,
    createdAt = createdAt,
)
