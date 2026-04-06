package com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRagConfigRepository : JpaRepository<RagConfigEntity, String> {
    fun findByOrganizationId(organizationId: String): RagConfigEntity?
}
