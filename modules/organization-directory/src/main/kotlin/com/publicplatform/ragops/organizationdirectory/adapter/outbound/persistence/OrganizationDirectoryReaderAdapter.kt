/**
 * LoadOrganizationPort의 JPA 구현체.
 *
 * 세션의 organizationIds 범위에 해당하는 기관 요약 목록을 반환한다.
 */
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
