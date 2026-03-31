package com.publicplatform.ragops.identityaccess.application.service

import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAdminUsersUseCase
import com.publicplatform.ragops.identityaccess.application.port.out.LoadAdminUsersPort
import com.publicplatform.ragops.identityaccess.domain.AdminUser

class GetAdminUsersService(
    private val loadAdminUsersPort: LoadAdminUsersPort,
) : GetAdminUsersUseCase {

    override fun listAll(): List<AdminUser> =
        loadAdminUsersPort.findAll()
}
