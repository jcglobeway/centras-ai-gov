package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.publicplatform.ragops.identityaccess.application.port.out.LoadAdminUsersPort
import com.publicplatform.ragops.identityaccess.domain.AdminUser

open class LoadAdminUsersPortAdapter(
    private val jpaRepository: JpaManageAdminUserPort,
) : LoadAdminUsersPort {

    override fun findAll(): List<AdminUser> =
        jpaRepository.findAll().map { it.toModel() }
}
