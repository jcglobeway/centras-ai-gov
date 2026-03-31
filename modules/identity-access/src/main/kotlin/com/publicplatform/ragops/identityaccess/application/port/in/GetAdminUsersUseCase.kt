package com.publicplatform.ragops.identityaccess.application.port.`in`

import com.publicplatform.ragops.identityaccess.domain.AdminUser

interface GetAdminUsersUseCase {
    fun listAll(): List<AdminUser>
}
