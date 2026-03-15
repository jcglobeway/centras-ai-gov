package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.AdminLoginCommand
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin/auth")
class AuthCommandController(
    private val developmentAdminSessionService: DevelopmentAdminSessionService,
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): LoginResponse {
        val result = developmentAdminSessionService.login(
            AdminLoginCommand(
                email = request.email,
                password = request.password,
                userAgent = httpRequest.getHeader("User-Agent"),
                ipAddress = httpRequest.remoteAddr,
            ),
        )

        return LoginResponse(
            user = LoginUser(
                id = result.session.snapshot.user.id,
                email = result.session.snapshot.user.email,
                displayName = result.session.snapshot.user.displayName,
                status = result.session.snapshot.user.status.name.lowercase(),
            ),
            session = LoginSession(
                token = result.session.sessionId,
                expiresAt = result.session.expiresAt,
            ),
            authorization = LoginAuthorization(
                primaryRole = result.primaryRoleCode,
                organizationScope = result.organizationScope,
                actions = result.session.snapshot.grantedActions,
            ),
        )
    }

    @PostMapping("/logout")
    fun logout(httpRequest: HttpServletRequest): LogoutResponse {
        val sessionId = adminRequestSessionResolver.requireSessionId(httpRequest)
        developmentAdminSessionService.logout(sessionId)
        return LogoutResponse(revoked = true, revokedAt = Instant.now())
    }
}

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val user: LoginUser,
    val session: LoginSession,
    val authorization: LoginAuthorization,
)

data class LoginUser(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
)

data class LoginSession(
    val token: String,
    val expiresAt: Instant,
)

data class LoginAuthorization(
    val primaryRole: String,
    val organizationScope: List<String>,
    val actions: List<String>,
)

data class LogoutResponse(
    val revoked: Boolean,
    val revokedAt: Instant,
)
