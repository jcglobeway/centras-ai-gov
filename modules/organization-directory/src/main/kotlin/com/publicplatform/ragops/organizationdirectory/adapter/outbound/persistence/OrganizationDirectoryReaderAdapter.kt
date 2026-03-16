package com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary
import com.publicplatform.ragops.organizationdirectory.application.port.out.LoadOrganizationPort

open class LoadOrganizationPortAdapter(
    private val jpaRepository: JpaOrganizationRepository,
) : LoadOrganizationPort {

    override fun getOrganizations(ids: Set<String>): List<OrganizationSummary> {
        return jpaRepository.findAllById(ids).map { it.toSummary() }
    }
}
