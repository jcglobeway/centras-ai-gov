package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.AdminUserStatus
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
