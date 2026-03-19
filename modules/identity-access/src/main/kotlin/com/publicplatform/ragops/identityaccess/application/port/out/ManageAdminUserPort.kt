package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AdminUser

interface ManageAdminUserPort {
    fun findByEmail(email: String): AdminUser?
    fun findById(userId: String): AdminUser?
    fun save(user: AdminUser): AdminUser
}
