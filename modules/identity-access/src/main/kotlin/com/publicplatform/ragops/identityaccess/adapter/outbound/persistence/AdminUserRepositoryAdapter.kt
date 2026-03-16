package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.publicplatform.ragops.identityaccess.domain.AdminUser
import com.publicplatform.ragops.identityaccess.application.port.out.ManageAdminUserPort

open class ManageAdminUserPortAdapter(
    private val jpaRepository: JpaManageAdminUserPort,
) : ManageAdminUserPort {

    override fun findByEmail(email: String): AdminUser? {
        return jpaRepository.findByEmail(email)?.toModel()
    }

    override fun findById(userId: String): AdminUser? {
        return jpaRepository.findById(userId).orElse(null)?.toModel()
    }

    override fun save(user: AdminUser): AdminUser {
        val entity = user.toEntity()
        val saved = jpaRepository.save(entity)
        return saved.toModel()
    }
}
