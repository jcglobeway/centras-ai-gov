package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRedteamBatchRunRepository : JpaRepository<RedteamBatchRunEntity, String> {
    fun findAllByOrganizationIdOrderByStartedAtDesc(
        organizationId: String,
        pageable: Pageable,
    ): List<RedteamBatchRunEntity>

    fun findAllByOrderByStartedAtDesc(pageable: Pageable): List<RedteamBatchRunEntity>
}
