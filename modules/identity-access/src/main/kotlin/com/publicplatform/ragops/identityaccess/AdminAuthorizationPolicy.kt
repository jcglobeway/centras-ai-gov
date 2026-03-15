package com.publicplatform.ragops.identityaccess

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

/**
 * 제품 API는 역할 이름이 아니라 action 코드와 조직 범위로 권한을 판정한다.
 * 이 정책을 모듈 경계 안에 두면 컨트롤러가 역할 하드코딩 없이 같은 규칙을 재사용할 수 있다.
 */
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
