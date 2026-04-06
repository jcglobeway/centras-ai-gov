/**
 * LoadOrganizationPort의 JPA 구현체.
 *
 * 세션의 organizationIds 범위에 해당하는 기관 요약 목록을 반환한다.
 */
package com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationScope
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary
import com.publicplatform.ragops.organizationdirectory.domain.Service
import com.publicplatform.ragops.organizationdirectory.application.port.out.LoadOrganizationPort

open class LoadOrganizationPortAdapter(
    private val jpaRepository: JpaOrganizationRepository,
    private val jpaServiceRepository: JpaServiceRepository,
) : LoadOrganizationPort {

    override fun getOrganizations(ids: Set<String>): List<OrganizationSummary> =
        jpaRepository.findAllById(ids).map { it.toSummary() }

    override fun listAll(): List<Organization> =
        jpaRepository.findAll().map { it.toModel() }

    override fun loadByScope(scope: OrganizationScope): List<Organization> =
        if (scope.globalAccess) {
            jpaRepository.findAll().map { it.toModel() }
        } else {
            jpaRepository.findAllById(scope.organizationIds).map { it.toModel() }
        }

    override fun listServicesByOrganizationId(organizationId: String): List<Service> =
        jpaServiceRepository.findByOrganizationId(organizationId).map { it.toModel() }
}
