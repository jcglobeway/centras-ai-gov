package com.publicplatform.ragops.identityaccess.domain

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
