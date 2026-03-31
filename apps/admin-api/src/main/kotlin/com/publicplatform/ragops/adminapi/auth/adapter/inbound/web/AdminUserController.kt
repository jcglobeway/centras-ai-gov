package com.publicplatform.ragops.adminapi.auth.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.application.port.`in`.GetAdminUsersUseCase
import com.publicplatform.ragops.identityaccess.domain.AdminUser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin")
class AdminUserController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val getAdminUsersUseCase: GetAdminUsersUseCase,
) {

    @GetMapping("/users")
    fun listUsers(servletRequest: HttpServletRequest): AdminUserListResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val users = getAdminUsersUseCase.listAll()
        return AdminUserListResponse(items = users.map { it.toResponse() }, total = users.size)
    }
}

data class AdminUserListResponse(val items: List<AdminUserResponse>, val total: Int)

data class AdminUserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
    val lastLoginAt: Instant?,
)

private fun AdminUser.toResponse() = AdminUserResponse(
    id = id,
    email = email,
    displayName = displayName,
    status = status.name.lowercase(),
    lastLoginAt = lastLoginAt,
)
