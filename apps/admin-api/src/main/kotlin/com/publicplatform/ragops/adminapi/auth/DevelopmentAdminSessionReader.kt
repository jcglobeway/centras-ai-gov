package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.AdminAuthErrorCode
import com.publicplatform.ragops.identityaccess.AdminAuthenticationException
import com.publicplatform.ragops.identityaccess.AdminCredentialAuthenticator
import com.publicplatform.ragops.identityaccess.AdminLoginCommand
import com.publicplatform.ragops.identityaccess.AdminLoginResult
import com.publicplatform.ragops.identityaccess.AdminRoleAssignment
import com.publicplatform.ragops.identityaccess.AdminSessionIssueCommand
import com.publicplatform.ragops.identityaccess.AdminSessionReader
import com.publicplatform.ragops.identityaccess.AdminSessionRecord
import com.publicplatform.ragops.identityaccess.AdminSessionRepository
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.AdminUser
import com.publicplatform.ragops.identityaccess.AdminUserStatus
import com.publicplatform.ragops.identityaccess.AuthenticatedAdminPrincipal
import com.publicplatform.ragops.identityaccess.SessionLookup
import com.publicplatform.ragops.identityaccess.defaultAdminSessionDuration
import com.publicplatform.ragops.identityaccess.isUsableAt
import com.publicplatform.ragops.organizationdirectory.OrganizationDirectoryReader
import com.publicplatform.ragops.organizationdirectory.OrganizationSummary
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class DevelopmentAdminSessionReader(
    private val adminSessionRepository: AdminSessionRepository,
    private val organizationDirectoryReader: OrganizationDirectoryReader,
) : AdminSessionReader {
    override fun restoreSession(lookup: SessionLookup): AdminSessionSnapshot {
        lookup.sessionId?.let { sessionId ->
            return restoreStoredSession(sessionId)
        }

        val roleCode = lookup.roleCodeHint?.ifBlank { null } ?: "ops_admin"
        val organizationId = lookup.organizationIdHint?.ifBlank { null } ?: defaultOrganizationFor(roleCode)
        val roleAssignments = listOf(
            AdminRoleAssignment(
                roleCode = roleCode,
                organizationId = organizationId,
            ),
        )

        return normalizeSessionScope(
            AdminSessionSnapshot(
                user = AdminUser(
                    id = lookup.userIdHint?.ifBlank { null } ?: "usr_dev_ops_001",
                    email = lookup.emailHint?.ifBlank { null } ?: "ops.admin@gov-platform.kr",
                    displayName = lookup.displayNameHint?.ifBlank { null } ?: "Operations Admin",
                    status = AdminUserStatus.ACTIVE,
                    lastLoginAt = Instant.parse("2026-03-15T09:00:00Z"),
                ),
                roleAssignments = roleAssignments,
                grantedActions = developmentActionsFor(roleCode),
            ),
        )
    }

    private fun restoreStoredSession(sessionId: String): AdminSessionSnapshot {
        val session = adminSessionRepository.findBySessionId(sessionId)
            ?: throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_UNAUTHORIZED,
                message = "Admin session could not be found.",
            )

        val now = Instant.now()
        if (session.revokedAt != null) {
            throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_SESSION_REVOKED,
                message = "Admin session has already been revoked.",
            )
        }

        if (!session.isUsableAt(now)) {
            throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_SESSION_EXPIRED,
                message = "Admin session has expired.",
            )
        }

        return normalizeSessionScope(session.snapshot)
    }

    // 저장소와 디버그 스텁이 섞여 있어도 조직 범위 판정 기준은 한 곳에서만 맞춘다.
    internal fun normalizeSessionScope(session: AdminSessionSnapshot): AdminSessionSnapshot {
        val knownOrganizationIds = organizationDirectoryReader
            .getOrganizations(session.roleAssignments.mapNotNull { it.organizationId }.toSet())
            .map { it.id }
            .toSet()

        return session.copy(
            roleAssignments = session.roleAssignments.map { assignment ->
                if (assignment.organizationId == null || assignment.organizationId in knownOrganizationIds) {
                    assignment
                } else {
                    assignment.copy(organizationId = null)
                }
            },
        )
    }

    private fun defaultOrganizationFor(roleCode: String): String? =
        when (roleCode) {
            "ops_admin" -> null
            else -> "org_seoul_120"
        }
}

@Service
class DevelopmentAdminSessionService(
    private val adminCredentialAuthenticator: AdminCredentialAuthenticator,
    private val adminSessionRepository: AdminSessionRepository,
    private val developmentAdminSessionReader: DevelopmentAdminSessionReader,
) {
    fun login(command: AdminLoginCommand): AdminLoginResult {
        val principal = adminCredentialAuthenticator.authenticate(command.email, command.password)
            ?: throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_INVALID_CREDENTIALS,
                message = "Admin email or password is invalid.",
            )

        val normalizedSnapshot = developmentAdminSessionReader.normalizeSessionScope(principal.snapshot)
        val issuedAt = Instant.now()
        val session = adminSessionRepository.issue(
            AdminSessionIssueCommand(
                snapshot = normalizedSnapshot,
                issuedAt = issuedAt,
                expiresAt = issuedAt.plus(defaultAdminSessionDuration()),
                userAgent = command.userAgent,
                ipAddress = command.ipAddress,
            ),
        )

        return AdminLoginResult(
            session = session,
            primaryRoleCode = normalizedSnapshot.roleAssignments.firstOrNull()?.roleCode ?: "unknown",
            organizationScope = normalizedSnapshot.roleAssignments.mapNotNull { it.organizationId }.distinct(),
        )
    }

    fun logout(sessionId: String) {
        val session = adminSessionRepository.findBySessionId(sessionId)
            ?: throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_UNAUTHORIZED,
                message = "Admin session could not be found.",
            )

        if (session.revokedAt != null) {
            throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_SESSION_REVOKED,
                message = "Admin session has already been revoked.",
            )
        }

        if (!session.isUsableAt(Instant.now())) {
            throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_SESSION_EXPIRED,
                message = "Admin session has expired.",
            )
        }

        adminSessionRepository.revoke(sessionId, Instant.now())
    }
}

@Service
class InMemoryOrganizationDirectoryReader : OrganizationDirectoryReader {
    private val organizations = listOf(
        OrganizationSummary(
            id = "org_seoul_120",
            name = "Seoul City Civil Service Center",
            institutionType = "metro",
        ),
        OrganizationSummary(
            id = "org_busan_220",
            name = "Busan Citizen Support Center",
            institutionType = "metro",
        ),
    )

    override fun getOrganizations(ids: Set<String>): List<OrganizationSummary> =
        organizations.filter { it.id in ids }
}

@Service
class InMemoryAdminSessionRepository : AdminSessionRepository, AdminCredentialAuthenticator {
    private val sessionSequence = AtomicLong(900)
    private val sessions = ConcurrentHashMap<String, AdminSessionRecord>(
        mapOf(
            "sess_ops_global_001" to sessionRecord(
                sessionId = "sess_ops_global_001",
                snapshot = snapshotFor(
                    userId = "usr_ops_global_001",
                    email = "ops.platform@gov-platform.kr",
                    displayName = "Platform Operator",
                    roleCode = "ops_admin",
                    organizationId = null,
                ),
                issuedAt = Instant.parse("2026-03-15T08:00:00Z"),
                expiresAt = Instant.parse("2026-03-15T18:00:00Z"),
            ),
            "sess_client_busan_001" to sessionRecord(
                sessionId = "sess_client_busan_001",
                snapshot = snapshotFor(
                    userId = "usr_client_busan_001",
                    email = "client.admin@busan.go.kr",
                    displayName = "Busan Client Admin",
                    roleCode = "client_admin",
                    organizationId = "org_busan_220",
                ),
                issuedAt = Instant.parse("2026-03-15T08:15:00Z"),
                expiresAt = Instant.parse("2026-03-15T18:15:00Z"),
            ),
            "sess_expired_qa_001" to sessionRecord(
                sessionId = "sess_expired_qa_001",
                snapshot = snapshotFor(
                    userId = "usr_qa_001",
                    email = "qa.manager@gov-platform.kr",
                    displayName = "QA Manager",
                    roleCode = "qa_admin",
                    organizationId = "org_seoul_120",
                ),
                issuedAt = Instant.parse("2026-03-14T00:00:00Z"),
                expiresAt = Instant.parse("2026-03-14T08:00:00Z"),
            ),
        ),
    )

    private val accounts = mapOf(
        "ops.platform@gov-platform.kr" to DevelopmentAdminAccount(
            password = "ops-pass-1234",
            snapshot = snapshotFor(
                userId = "usr_ops_global_001",
                email = "ops.platform@gov-platform.kr",
                displayName = "Platform Operator",
                roleCode = "ops_admin",
                organizationId = null,
            ),
        ),
        "client.admin@busan.go.kr" to DevelopmentAdminAccount(
            password = "client-pass-1234",
            snapshot = snapshotFor(
                userId = "usr_client_busan_001",
                email = "client.admin@busan.go.kr",
                displayName = "Busan Client Admin",
                roleCode = "client_admin",
                organizationId = "org_busan_220",
            ),
        ),
        "qa.manager@gov-platform.kr" to DevelopmentAdminAccount(
            password = "qa-pass-1234",
            snapshot = snapshotFor(
                userId = "usr_qa_001",
                email = "qa.manager@gov-platform.kr",
                displayName = "QA Manager",
                roleCode = "qa_admin",
                organizationId = "org_seoul_120",
            ),
        ),
    )

    override fun findBySessionId(sessionId: String): AdminSessionRecord? =
        sessions[sessionId]

    override fun issue(command: AdminSessionIssueCommand): AdminSessionRecord {
        val sessionId = "sess_dev_${sessionSequence.incrementAndGet()}"
        val record = sessionRecord(
            sessionId = sessionId,
            snapshot = command.snapshot,
            issuedAt = command.issuedAt,
            expiresAt = command.expiresAt,
        )

        sessions[sessionId] = record
        return record
    }

    override fun revoke(sessionId: String, revokedAt: Instant): AdminSessionRecord? {
        val existing = sessions[sessionId] ?: return null
        val updated = existing.copy(revokedAt = revokedAt)
        sessions[sessionId] = updated
        return updated
    }

    override fun authenticate(email: String, password: String): AuthenticatedAdminPrincipal? =
        accounts[email]
            ?.takeIf { it.password == password }
            ?.let { AuthenticatedAdminPrincipal(snapshot = it.snapshot) }

    private fun snapshotFor(
        userId: String,
        email: String,
        displayName: String,
        roleCode: String,
        organizationId: String?,
    ): AdminSessionSnapshot =
        AdminSessionSnapshot(
            user = AdminUser(
                id = userId,
                email = email,
                displayName = displayName,
                status = AdminUserStatus.ACTIVE,
                lastLoginAt = Instant.parse("2026-03-15T08:30:00Z"),
            ),
            roleAssignments = listOf(
                AdminRoleAssignment(
                    roleCode = roleCode,
                    organizationId = organizationId,
                ),
            ),
            grantedActions = developmentActionsFor(roleCode),
        )

    private fun sessionRecord(
        sessionId: String,
        snapshot: AdminSessionSnapshot,
        issuedAt: Instant,
        expiresAt: Instant,
        revokedAt: Instant? = null,
    ): AdminSessionRecord =
        AdminSessionRecord(
            sessionId = sessionId,
            snapshot = snapshot,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            lastSeenAt = issuedAt,
            revokedAt = revokedAt,
        )
}

private data class DevelopmentAdminAccount(
    val password: String,
    val snapshot: AdminSessionSnapshot,
)

@Configuration
class IdentityAccessDevelopmentConfig {
    @Bean
    fun adminAuthorizationPolicy() = com.publicplatform.ragops.identityaccess.AdminAuthorizationPolicy()
}

private fun developmentActionsFor(roleCode: String): List<String> =
    when (roleCode) {
        "ops_admin" -> listOf(
            "dashboard.read",
            "organization.read",
            "organization.update",
            "crawl_source.read",
            "crawl_source.write",
            "ingestion_job.read",
            "document.read",
            "document.reingest.request",
            "document.reindex.request",
            "document.reindex.execute",
            "qa.review.read",
            "qa.review.write",
            "metrics.read",
            "auth.user.read",
            "auth.role.assign",
        )

        "qa_admin" -> listOf(
            "dashboard.read",
            "crawl_source.read",
            "ingestion_job.read",
            "document.read",
            "document.reindex.request",
            "qa.review.read",
            "qa.review.write",
            "metrics.read",
        )

        "client_admin" -> listOf(
            "dashboard.read",
            "crawl_source.read",
            "ingestion_job.read",
            "document.read",
            "document.reingest.request",
            "document.reindex.request",
            "metrics.read",
        )

        else -> listOf("dashboard.read")
    }
