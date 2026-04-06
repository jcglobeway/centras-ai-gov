package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface JpaAlertEventRepository : JpaRepository<AlertEventEntity, String> {
    @Query("SELECT e FROM AlertEventEntity e ORDER BY e.triggeredAt DESC LIMIT :limit")
    fun findRecent(limit: Int): List<AlertEventEntity>
}
