package com.publicplatform.ragops.identityaccess

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaAdminUserRepository : JpaRepository<AdminUserEntity, String> {
    fun findByEmail(email: String): AdminUserEntity?
}
