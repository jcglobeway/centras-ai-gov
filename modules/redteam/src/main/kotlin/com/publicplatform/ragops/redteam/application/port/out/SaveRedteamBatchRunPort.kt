package com.publicplatform.ragops.redteam.application.port.out

import com.publicplatform.ragops.redteam.domain.BatchRunStatus
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary
import java.time.Instant

interface SaveRedteamBatchRunPort {
    fun save(run: RedteamBatchRunSummary): RedteamBatchRunSummary
    fun updateResult(
        runId: String,
        passCount: Int,
        failCount: Int,
        passRate: Double,
        completedAt: Instant,
        status: BatchRunStatus,
    )
}
