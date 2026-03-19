/**
 * 관리자 액션·범위 기반 권한 정책 도메인 서비스.
 *
 * requireAuthorized()는 grantedActions 목록과 기관 범위를 검사하며,
 * 위반 시 ACTION_FORBIDDEN 또는 SCOPE_FORBIDDEN 이유가 담긴 예외를 던진다.
 * 외부 의존성 없이 세션 스냅샷만으로 동작하므로 단위 테스트가 용이하다.
 */
package com.publicplatform.ragops.identityaccess.domain

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
