package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.SessionLookup
import com.publicplatform.ragops.identityaccess.application.port.out.RestoreSessionPort
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class AdminRequestSessionResolver(
    private val adminSessionReader: RestoreSessionPort,
) {
    fun resolve(request: HttpServletRequest): AdminSessionSnapshot =
        adminSessionReader.restoreSession(
            SessionLookup(
                sessionId = request.headerOrNull("X-Admin-Session-Id"),
                userIdHint = request.headerOrNull("X-Debug-User-Id"),
                emailHint = request.headerOrNull("X-Debug-Email"),
                displayNameHint = request.headerOrNull("X-Debug-Display-Name"),
                roleCodeHint = request.headerOrNull("X-Debug-Role"),
                organizationIdHint = request.headerOrNull("X-Debug-Organization-Id"),
            ),
        )

    fun requireSessionId(request: HttpServletRequest): String =
        request.headerOrNull("X-Admin-Session-Id")
            ?: throw UnauthorizedException(
                code = "AUTH_UNAUTHORIZED",
                message = "Admin session header is required.",
            )
}

fun HttpServletRequest.headerOrNull(name: String): String? =
    getHeader(name)?.takeIf { it.isNotBlank() }
