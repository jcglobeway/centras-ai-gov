package com.publicplatform.ragops.identityaccess

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryAdminSessionRepository : AdminSessionRepository {
    private val sessions = ConcurrentHashMap<String, AdminSessionRecord>()

    override fun findBySessionId(sessionId: String): AdminSessionRecord? {
        return sessions[sessionId]
    }

    override fun issue(command: AdminSessionIssueCommand): AdminSessionRecord {
        val sessionId = UUID.randomUUID().toString()
        val record = AdminSessionRecord(
            sessionId = sessionId,
            snapshot = command.snapshot,
            issuedAt = command.issuedAt,
            expiresAt = command.expiresAt,
            lastSeenAt = command.issuedAt,
            revokedAt = null,
        )
        sessions[sessionId] = record
        return record
    }

    override fun revoke(sessionId: String, revokedAt: Instant): AdminSessionRecord? {
        val existing = sessions[sessionId] ?: return null
        val revoked = existing.copy(revokedAt = revokedAt)
        sessions[sessionId] = revoked
        return revoked
    }
}
