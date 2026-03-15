package com.publicplatform.ragops.identityaccess

import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class AdminSessionRepositoryAdapter(
    private val jpaRepository: JpaAdminSessionRepository,
) : AdminSessionRepository {

    override fun findBySessionId(sessionId: String): AdminSessionRecord? {
        return jpaRepository.findById(sessionId).orElse(null)?.toModel()
    }

    @Transactional
    override fun issue(command: AdminSessionIssueCommand): AdminSessionRecord {
        val sessionId = UUID.randomUUID().toString()
        val sessionTokenHash = sessionId // 개발 환경: sessionId를 hash로 사용

        val record = AdminSessionRecord(
            sessionId = sessionId,
            snapshot = command.snapshot,
            issuedAt = command.issuedAt,
            expiresAt = command.expiresAt,
            lastSeenAt = command.issuedAt,
            revokedAt = null,
        )

        val entity = record.toEntity(
            sessionTokenHash = sessionTokenHash,
            ipAddress = command.ipAddress,
            userAgent = command.userAgent,
        )

        jpaRepository.save(entity)
        return record
    }

    @Transactional
    override fun revoke(sessionId: String, revokedAt: Instant): AdminSessionRecord? {
        val entity = jpaRepository.findById(sessionId).orElse(null) ?: return null
        val revoked = AdminSessionEntity(
            id = entity.id,
            userId = entity.userId,
            sessionTokenHash = entity.sessionTokenHash,
            snapshotJson = entity.snapshotJson,
            issuedAt = entity.issuedAt,
            expiresAt = entity.expiresAt,
            lastSeenAt = entity.lastSeenAt,
            revokedAt = revokedAt,
            ipAddress = entity.ipAddress,
            userAgent = entity.userAgent,
            createdAt = entity.createdAt,
        )
        jpaRepository.save(revoked)
        return revoked.toModel()
    }
}
