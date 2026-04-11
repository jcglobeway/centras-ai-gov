package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRedteamCaseRepository : JpaRepository<RedteamCaseEntity, String> {
    fun findAllByOrderByCreatedAtDesc(): List<RedteamCaseEntity>
    fun findAllByIsActiveTrueOrderByCreatedAtDesc(): List<RedteamCaseEntity>
}
