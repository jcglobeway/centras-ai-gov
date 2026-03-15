package com.publicplatform.ragops.identityaccess

import java.time.Instant
import java.time.Duration

enum class AdminUserStatus {
    ACTIVE,
    INVITED,
    SUSPENDED,
}

data class AdminUser(
    val id: String,
    val email: String,
    val displayName: String,
    val status: AdminUserStatus,
    val lastLoginAt: Instant?,
)

data class AdminRoleAssignment(
    val roleCode: String,
    val organizationId: String?,
)

data class AdminSessionSnapshot(
    val user: AdminUser,
    val roleAssignments: List<AdminRoleAssignment>,
    val grantedActions: List<String>,
)

data class AdminSessionRecord(
    val sessionId: String,
    val snapshot: AdminSessionSnapshot,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val lastSeenAt: Instant,
    val revokedAt: Instant?,
)

data class AdminSessionIssueCommand(
    val snapshot: AdminSessionSnapshot,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val userAgent: String?,
    val ipAddress: String?,
)

data class AdminLoginCommand(
    val email: String,
    val password: String,
    val userAgent: String?,
    val ipAddress: String?,
)

data class AdminLoginResult(
    val session: AdminSessionRecord,
    val primaryRoleCode: String,
    val organizationScope: List<String>,
)

data class AuthenticatedAdminPrincipal(
    val snapshot: AdminSessionSnapshot,
)

enum class AdminAuthErrorCode {
    AUTH_UNAUTHORIZED,
    AUTH_INVALID_CREDENTIALS,
    AUTH_SESSION_EXPIRED,
    AUTH_SESSION_REVOKED,
}

class AdminAuthenticationException(
    val code: AdminAuthErrorCode,
    override val message: String,
) : RuntimeException(message)

data class SessionLookup(
    val sessionId: String?,
    val userIdHint: String?,
    val emailHint: String?,
    val displayNameHint: String?,
    val roleCodeHint: String?,
    val organizationIdHint: String?,
)

interface AdminSessionReader {
    fun restoreSession(lookup: SessionLookup): AdminSessionSnapshot
}

interface AdminSessionRepository {
    fun findBySessionId(sessionId: String): AdminSessionRecord?
    fun issue(command: AdminSessionIssueCommand): AdminSessionRecord
    fun revoke(sessionId: String, revokedAt: Instant): AdminSessionRecord?
}

interface AdminCredentialAuthenticator {
    fun authenticate(email: String, password: String): AuthenticatedAdminPrincipal?
}

interface AdminUserRepository {
    fun findByEmail(email: String): AdminUser?
    fun findById(userId: String): AdminUser?
    fun save(user: AdminUser): AdminUser
}

data class AuditLogEntry(
    val id: String,
    val actorUserId: String?,
    val actorRoleCode: String?,
    val organizationId: String?,
    val actionCode: String,
    val resourceType: String?,
    val resourceId: String?,
    val requestId: String?,
    val traceId: String?,
    val resultCode: String,
    val createdAt: Instant,
)

interface AuditLogRepository {
    fun save(entry: AuditLogEntry): AuditLogEntry
}

fun AdminSessionRecord.isUsableAt(at: Instant): Boolean =
    revokedAt == null && expiresAt.isAfter(at)

fun defaultAdminSessionDuration(): Duration = Duration.ofHours(8)
