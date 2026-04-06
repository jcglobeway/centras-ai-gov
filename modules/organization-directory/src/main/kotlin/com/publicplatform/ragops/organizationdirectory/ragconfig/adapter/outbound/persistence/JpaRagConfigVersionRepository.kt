package com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRagConfigVersionRepository : JpaRepository<RagConfigVersionEntity, String> {
    fun findByOrganizationIdOrderByVersionDesc(organizationId: String): List<RagConfigVersionEntity>
    fun findByOrganizationIdAndVersion(organizationId: String, version: Int): RagConfigVersionEntity?
}
