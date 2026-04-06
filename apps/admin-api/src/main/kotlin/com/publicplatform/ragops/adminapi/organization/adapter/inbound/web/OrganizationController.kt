/**
 * 기관 목록 조회 HTTP 인바운드 어댑터.
 *
 * 세션에서 기관 범위를 추출하여 GetOrganizationsUseCase에 위임하고,
 * organization.read 권한이 없으면 403을 반환한다.
 */
package com.publicplatform.ragops.adminapi.organization.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.organizationdirectory.application.port.`in`.GetOrganizationsUseCase
import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationScope
import com.publicplatform.ragops.organizationdirectory.domain.Service
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @GetMapping("/organizations/{id}/services")
    fun listServices(
        @PathVariable id: String,
        request: HttpServletRequest,
    ): ServiceListResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode = "organization.read"))
        val items = getOrganizationsUseCase.listServices(id).map { it.toResponse() }
        return ServiceListResponse(items = items, total = items.size)
    }
}

data class OrganizationListResponse(val items: List<OrganizationResponse>, val total: Int)
data class ServiceListResponse(val items: List<ServiceResponse>, val total: Int)

data class ServiceResponse(
    val serviceId: String,
    val organizationId: String,
    val name: String,
    val channelType: String,
    val status: String,
)

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

private fun Service.toResponse() = ServiceResponse(
    serviceId = id,
    organizationId = organizationId,
    name = name,
    channelType = channelType,
    status = status,
)

private fun Organization.toResponse() = OrganizationResponse(
    organizationId = id,
    name = name,
    code = orgCode,
    status = status,
    institutionType = institutionType,
    createdAt = createdAt,
)
