package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AdminUser

interface LoadAdminUsersPort {
    fun findAll(): List<AdminUser>
}
