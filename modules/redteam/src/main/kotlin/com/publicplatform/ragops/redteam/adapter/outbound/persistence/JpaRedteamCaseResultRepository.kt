package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRedteamCaseResultRepository : JpaRepository<RedteamCaseResultEntity, String> {
    fun findAllByBatchRunIdOrderByJudgmentAsc(batchRunId: String): List<RedteamCaseResultEntity>
}
