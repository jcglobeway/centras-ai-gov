/**
 * identity-access 바운디드 컨텍스트의 도메인 모델.
 *
 * 관리자 사용자, 세션, 권한 정책, 감사 로그에 관한 순수 비즈니스 개념을 정의한다.
 * AdminAuthorizationPolicy는 역할 기반 권한 체크 로직을 캡슐화하며 외부 의존성이 없다.
 */
package com.publicplatform.ragops.identityaccess.domain

import java.time.Duration
import java.time.Instant

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

enum class AuthorizationFailureReason {
    ACTION_FORBIDDEN,
    SCOPE_FORBIDDEN,
}

data class AuthorizationCheck(
    val actionCode: String,
    val organizationId: String? = null,
)

class AdminAuthorizationException(
    val reason: AuthorizationFailureReason,
    override val message: String,
) : RuntimeException(message)

class AdminAuthorizationPolicy {
    fun requireAuthorized(
        session: AdminSessionSnapshot,
        check: AuthorizationCheck,
    ) {
        if (check.actionCode !in session.grantedActions) {
            throw AdminAuthorizationException(
                reason = AuthorizationFailureReason.ACTION_FORBIDDEN,
                message = "요청한 액션에 대한 권한이 없습니다: ${check.actionCode}",
            )
        }

        val requestedOrganizationId = check.organizationId ?: return
        val organizationScope = session.roleAssignments.mapNotNull { it.organizationId }.toSet()
        val hasGlobalScope = session.roleAssignments.any { it.organizationId == null }

        if (!hasGlobalScope && requestedOrganizationId !in organizationScope) {
            throw AdminAuthorizationException(
                reason = AuthorizationFailureReason.SCOPE_FORBIDDEN,
                message = "해당 기관 범위에 접근할 수 없습니다: $requestedOrganizationId",
            )
        }
    }
}

fun AdminSessionRecord.isUsableAt(at: Instant): Boolean =
    revokedAt == null && expiresAt.isAfter(at)

fun defaultAdminSessionDuration(): Duration = Duration.ofHours(8)
