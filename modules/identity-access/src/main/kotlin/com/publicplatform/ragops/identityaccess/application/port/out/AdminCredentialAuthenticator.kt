package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AuthenticatedAdminPrincipal

interface AdminCredentialAuthenticator {
    fun authenticate(email: String, password: String): AuthenticatedAdminPrincipal?
}
