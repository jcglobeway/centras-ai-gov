/**
 * 현재 세션 정보 조회 HTTP 인바운드 어댑터.
 *
 * 유효한 세션 토큰으로 호출하면 로그인한 사용자의 프로필과 역할을 반환한다.
 */
package com.publicplatform.ragops.adminapi.auth.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AdminUserStatus
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin/auth")
class AuthMeController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
) {
    @GetMapping("/me")
    fun me(request: HttpServletRequest): AuthMeResponse =
        adminRequestSessionResolver.resolve(request).toResponse()
}

private fun AdminSessionSnapshot.toResponse(): AuthMeResponse =
    AuthMeResponse(
        user = AuthUser(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
            status = user.status.toApiValue(),
            lastLoginAt = user.lastLoginAt,
        ),
        roles = roleAssignments.map {
            AuthRole(
                roleCode = it.roleCode,
                organizationId = it.organizationId,
            )
        },
        actions = grantedActions,
    )

private fun AdminUserStatus.toApiValue(): String =
    when (this) {
        AdminUserStatus.ACTIVE -> "active"
        AdminUserStatus.INVITED -> "invited"
        AdminUserStatus.SUSPENDED -> "suspended"
    }

data class AuthMeResponse(
    val user: AuthUser,
    val roles: List<AuthRole>,
    val actions: List<String>,
)

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
    val lastLoginAt: Instant?,
)

data class AuthRole(
    val roleCode: String,
    val organizationId: String?,
)
