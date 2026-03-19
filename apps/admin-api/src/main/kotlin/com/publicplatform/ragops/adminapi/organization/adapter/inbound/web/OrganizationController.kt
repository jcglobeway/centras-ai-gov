package com.publicplatform.ragops.adminapi.organization.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.organizationdirectory.application.port.`in`.GetOrganizationsUseCase
import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationScope
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin")
class OrganizationController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val getOrganizationsUseCase: GetOrganizationsUseCase,
) {
    @GetMapping("/organizations")
    fun listOrganizations(request: HttpServletRequest): OrganizationListResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode = "organization.read"))
        val items = getOrganizationsUseCase.listOrganizations(session.toScope()).map { it.toResponse() }
        return OrganizationListResponse(items = items, total = items.size)
    }
}

data class OrganizationListResponse(val items: List<OrganizationResponse>, val total: Int)

data class OrganizationResponse(
    val organizationId: String,
    val name: String,
    val code: String,
    val status: String,
    val institutionType: String,
    val createdAt: Instant,
)

private fun AdminSessionSnapshot.toScope() = OrganizationScope(
    organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
    globalAccess = roleAssignments.any { it.organizationId == null },
)

private fun Organization.toResponse() = OrganizationResponse(
    organizationId = id,
    name = name,
    code = orgCode,
    status = status,
    institutionType = institutionType,
    createdAt = createdAt,
)
