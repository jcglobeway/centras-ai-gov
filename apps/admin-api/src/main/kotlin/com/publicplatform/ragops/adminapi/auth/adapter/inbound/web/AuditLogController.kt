package com.publicplatform.ragops.adminapi.auth.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.application.port.`in`.AuditLogFilter
import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAuditLogsUseCase
import com.publicplatform.ragops.identityaccess.application.port.`in`.RecordAuditLogCommand
import com.publicplatform.ragops.identityaccess.application.port.`in`.RecordAuditLogUseCase
import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate

@RestController
@RequestMapping("/admin")
class AuditLogController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val getAuditLogsUseCase: GetAuditLogsUseCase,
    private val recordAuditLogUseCase: RecordAuditLogUseCase,
) {

    @GetMapping("/audit-logs")
    fun listAuditLogs(
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("page_size", defaultValue = "20") pageSize: Int,
        @RequestParam("from", required = false) from: String?,
        @RequestParam("to", required = false) to: String?,
        @RequestParam("action_code", required = false) actionCode: String?,
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("actor_user_id", required = false) actorUserId: String?,
        servletRequest: HttpServletRequest,
    ): AuditLogListResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val filter = AuditLogFilter(
            from = from?.let { LocalDate.parse(it) },
            to = to?.let { LocalDate.parse(it) },
            actionCode = actionCode,
            organizationId = organizationId,
            actorUserId = actorUserId,
        )
        val result = getAuditLogsUseCase.list(filter, page, pageSize)
        return AuditLogListResponse(
            items = result.items.map { it.toResponse() },
            total = result.total.toInt(),
        )
    }

    @GetMapping("/audit-logs/export.csv")
    fun exportAuditLogsCsv(
        @RequestParam("from", required = false) from: String?,
        @RequestParam("to", required = false) to: String?,
        @RequestParam("action_code", required = false) actionCode: String?,
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("actor_user_id", required = false) actorUserId: String?,
        servletRequest: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        adminRequestSessionResolver.resolve(servletRequest)
        val filter = AuditLogFilter(
            from = from?.let { LocalDate.parse(it) },
            to = to?.let { LocalDate.parse(it) },
            actionCode = actionCode,
            organizationId = organizationId,
            actorUserId = actorUserId,
        )
        val items = getAuditLogsUseCase.listAll(filter)
        response.contentType = "text/csv; charset=UTF-8"
        response.setHeader("Content-Disposition", "attachment; filename=\"audit-logs.csv\"")
        val writer = response.writer
        writer.println("id,actor_user_id,actor_role_code,organization_id,action_code,resource_type,resource_id,result_code,created_at")
        items.forEach { log ->
            writer.println(
                listOf(
                    log.id, log.actorUserId ?: "", log.actorRoleCode ?: "",
                    log.organizationId ?: "", log.actionCode,
                    log.resourceType ?: "", log.resourceId ?: "",
                    log.resultCode, log.createdAt.toString(),
                ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
            )
        }
        writer.flush()
    }

    @PostMapping("/audit-logs")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAuditLog(
        @RequestBody request: CreateAuditLogRequest,
    ): AuditLogCreatedResponse {
        val entry = recordAuditLogUseCase.record(
            RecordAuditLogCommand(
                actionCode = request.actionCode,
                organizationId = request.organizationId,
                actorUserId = request.actorUserId,
                actorRoleCode = request.actorRoleCode,
                resourceType = request.resourceType,
                resourceId = request.resourceId,
                resultCode = request.resultCode ?: "success",
            )
        )
        return AuditLogCreatedResponse(id = entry.id, saved = true)
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

data class CreateAuditLogRequest(
    val actionCode: String,
    val organizationId: String? = null,
    val actorUserId: String? = null,
    val actorRoleCode: String? = null,
    val resourceType: String? = null,
    val resourceId: String? = null,
    val resultCode: String? = null,
)

data class AuditLogCreatedResponse(val id: String, val saved: Boolean)

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
