package com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaServiceRepository : JpaRepository<ServiceEntity, String> {
    fun findByOrganizationId(organizationId: String): List<ServiceEntity>
}
