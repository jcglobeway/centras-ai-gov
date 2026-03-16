/**
 * AuditLog DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry
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
        id = id, actorUserId = actorUserId, actorRoleCode = actorRoleCode,
        organizationId = organizationId, actionCode = actionCode,
        resourceType = resourceType, resourceId = resourceId,
        requestId = requestId, traceId = traceId, resultCode = resultCode, createdAt = createdAt,
    )

fun AuditLogEntry.toEntity(): AuditLogEntity =
    AuditLogEntity(
        id = id, actorUserId = actorUserId, actorRoleCode = actorRoleCode,
        organizationId = organizationId, actionCode = actionCode,
        resourceType = resourceType, resourceId = resourceId,
        requestId = requestId, traceId = traceId, resultCode = resultCode, createdAt = createdAt,
    )
