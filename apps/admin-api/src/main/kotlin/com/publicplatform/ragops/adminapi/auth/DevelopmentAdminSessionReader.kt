/**
 * 개발 환경용 RestoreSessionPort 구현체.
 *
 * H2 인메모리 DB + JPA 어댑터를 사용하여 세션을 복원한다.
 * 운영 환경에서는 Redis 기반 구현체로 교체할 수 있다.
 */
package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.domain.AdminAuthErrorCode
import com.publicplatform.ragops.identityaccess.domain.AdminAuthenticationException
import com.publicplatform.ragops.identityaccess.domain.AdminLoginCommand
import com.publicplatform.ragops.identityaccess.domain.AdminLoginResult
import com.publicplatform.ragops.identityaccess.domain.AdminRoleAssignment
import com.publicplatform.ragops.identityaccess.domain.AdminSessionIssueCommand
import com.publicplatform.ragops.identityaccess.domain.AdminSessionRecord
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AdminUser
import com.publicplatform.ragops.identityaccess.domain.AdminUserStatus
import com.publicplatform.ragops.identityaccess.domain.AuthenticatedAdminPrincipal
import com.publicplatform.ragops.identityaccess.domain.SessionLookup
import com.publicplatform.ragops.identityaccess.domain.defaultAdminSessionDuration
import com.publicplatform.ragops.identityaccess.domain.isUsableAt
import com.publicplatform.ragops.identityaccess.application.port.`in`.AdminAuthUseCase
import com.publicplatform.ragops.identityaccess.application.port.out.AdminCredentialAuthenticator
import com.publicplatform.ragops.identityaccess.application.port.out.RestoreSessionPort
import com.publicplatform.ragops.identityaccess.application.port.out.ManageAdminSessionPort
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary
import com.publicplatform.ragops.organizationdirectory.application.port.out.LoadOrganizationPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DevelopmentRestoreSessionPort(
    private val adminSessionRepository: ManageAdminSessionPort,
    private val organizationDirectoryReader: LoadOrganizationPort,
) : RestoreSessionPort {
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
            "ops_admin", "super_admin" -> null
            else -> "org_seoul_120"
        }
}

class DevelopmentAdminSessionService(
    private val adminCredentialAuthenticator: AdminCredentialAuthenticator,
    private val adminSessionRepository: ManageAdminSessionPort,
    private val developmentRestoreSessionPort: DevelopmentRestoreSessionPort,
) : AdminAuthUseCase {
    override fun login(command: AdminLoginCommand): AdminLoginResult {
        val principal = adminCredentialAuthenticator.authenticate(command.email, command.password)
            ?: throw AdminAuthenticationException(
                code = AdminAuthErrorCode.AUTH_INVALID_CREDENTIALS,
                message = "Admin email or password is invalid.",
            )

        val normalizedSnapshot = developmentRestoreSessionPort.normalizeSessionScope(principal.snapshot)
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

    override fun logout(sessionId: String) {
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

@Configuration
class IdentityAccessDevelopmentConfig {
    @Bean
    fun adminAuthorizationPolicy() = com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy()
}

internal fun developmentActionsFor(roleCode: String): List<String> =
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
            "metrics.aggregation.trigger",
            "auth.user.read",
            "auth.role.assign",
            "redteam.case.read",
            "redteam.case.write",
            "redteam.case.delete",
            "redteam.batch.run",
            "redteam.batch.read",
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
            "redteam.case.read",
            "redteam.case.write",
            "redteam.batch.run",
            "redteam.batch.read",
        )

        "client_admin", "client_org_admin" -> listOf(
            "dashboard.read",
            "crawl_source.read",
            "ingestion_job.read",
            "document.read",
            "document.reingest.request",
            "document.reindex.request",
            "metrics.read",
        )

        "super_admin" -> listOf(
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
            "metrics.aggregation.trigger",
            "auth.user.read",
            "auth.role.assign",
            "redteam.case.read",
            "redteam.case.write",
            "redteam.case.delete",
            "redteam.batch.run",
            "redteam.batch.read",
        )

        "client_viewer" -> listOf(
            "dashboard.read",
            "metrics.read",
        )

        "knowledge_editor" -> listOf(
            "dashboard.read",
            "document.read",
            "document.reingest.request",
            "document.reindex.request",
        )

        else -> listOf("dashboard.read")
    }
