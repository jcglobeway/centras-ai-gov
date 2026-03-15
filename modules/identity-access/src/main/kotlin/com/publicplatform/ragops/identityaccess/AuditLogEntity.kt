package com.publicplatform.ragops.identityaccess

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "audit_logs")
class AuditLogEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "actor_user_id")
    val actorUserId: String?,

    @Column(name = "actor_role_code")
    val actorRoleCode: String?,

    @Column(name = "organization_id")
    val organizationId: String?,

    @Column(name = "action_code", nullable = false)
    val actionCode: String,

    @Column(name = "resource_type")
    val resourceType: String?,

    @Column(name = "resource_id")
    val resourceId: String?,

    @Column(name = "request_id")
    val requestId: String?,

    @Column(name = "trace_id")
    val traceId: String?,

    @Column(name = "result_code", nullable = false)
    val resultCode: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

fun AuditLogEntity.toModel(): AuditLogEntry =
    AuditLogEntry(
        id = id,
        actorUserId = actorUserId,
        actorRoleCode = actorRoleCode,
        organizationId = organizationId,
        actionCode = actionCode,
        resourceType = resourceType,
        resourceId = resourceId,
        requestId = requestId,
        traceId = traceId,
        resultCode = resultCode,
        createdAt = createdAt,
    )

fun AuditLogEntry.toEntity(): AuditLogEntity =
    AuditLogEntity(
        id = id,
        actorUserId = actorUserId,
        actorRoleCode = actorRoleCode,
        organizationId = organizationId,
        actionCode = actionCode,
        resourceType = resourceType,
        resourceId = resourceId,
        requestId = requestId,
        traceId = traceId,
        resultCode = resultCode,
        createdAt = createdAt,
    )
