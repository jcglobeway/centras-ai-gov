/**
 * ManageOrganizationPort의 JPA 구현체.
 *
 * 기관 단건 조회 및 저장을 제공한다.
 */
package com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.application.port.out.OrganizationRepository

open class OrganizationRepositoryAdapter(
    private val jpaRepository: JpaOrganizationRepository,
) : OrganizationRepository {

    override fun findAll(): List<Organization> = jpaRepository.findAll().map { it.toModel() }

    override fun findById(id: String): Organization? = jpaRepository.findById(id).orElse(null)?.toModel()

    override fun save(organization: Organization): Organization {
        val entity = organization.toEntity()
        val saved = jpaRepository.save(entity)
        return saved.toModel()
    }
}
