/**
 * AdminSession DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.publicplatform.ragops.identityaccess.domain.AdminSessionRecord
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "admin_sessions")
class AdminSessionEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "session_token_hash", nullable = false)
    val sessionTokenHash: String,

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    val snapshotJson: String,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "last_seen_at", nullable = false)
    val lastSeenAt: Instant,

    @Column(name = "revoked_at")
    val revokedAt: Instant?,

    @Column(name = "ip_address")
    val ipAddress: String?,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
    findAndRegisterModules()
}

fun AdminSessionEntity.toModel(): AdminSessionRecord {
    val snapshot = objectMapper.readValue<AdminSessionSnapshot>(snapshotJson)
    return AdminSessionRecord(
        sessionId = id,
        snapshot = snapshot,
        issuedAt = issuedAt,
        expiresAt = expiresAt,
        lastSeenAt = lastSeenAt,
        revokedAt = revokedAt,
    )
}

fun AdminSessionRecord.toEntity(sessionTokenHash: String, ipAddress: String?, userAgent: String?): AdminSessionEntity {
    val snapshotJson = objectMapper.writeValueAsString(snapshot)
    return AdminSessionEntity(
        id = sessionId,
        userId = snapshot.user.id,
        sessionTokenHash = sessionTokenHash,
        snapshotJson = snapshotJson,
        issuedAt = issuedAt,
        expiresAt = expiresAt,
        lastSeenAt = lastSeenAt,
        revokedAt = revokedAt,
        ipAddress = ipAddress,
        userAgent = userAgent,
        createdAt = issuedAt,
    )
}
