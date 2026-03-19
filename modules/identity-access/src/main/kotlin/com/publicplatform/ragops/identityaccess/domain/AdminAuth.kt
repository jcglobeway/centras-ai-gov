/**
 * 관리자 인증 커맨드·결과·에러 코드 도메인 모델.
 *
 * AdminAuthErrorCode는 HTTP 상태 코드와 1:1로 매핑되며 AuthExceptionHandler가 처리한다.
 * AdminLoginResult는 로그인 성공 시 세션·역할·기관 범위를 응답에 포함하기 위해 사용된다.
 */
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
