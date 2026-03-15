package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.AdminSessionReader
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.SessionLookup
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class AdminRequestSessionResolver(
    private val adminSessionReader: AdminSessionReader,
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
}

fun HttpServletRequest.headerOrNull(name: String): String? =
    getHeader(name)?.takeIf { it.isNotBlank() }
