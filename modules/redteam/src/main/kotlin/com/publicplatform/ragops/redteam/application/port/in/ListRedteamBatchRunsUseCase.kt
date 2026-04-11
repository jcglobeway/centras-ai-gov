package com.publicplatform.ragops.redteam.application.port.`in`

import com.publicplatform.ragops.redteam.domain.RedteamBatchRunDetail
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary

interface ListRedteamBatchRunsUseCase {
    fun listRuns(organizationId: String? = null): List<RedteamBatchRunSummary>
    fun getRunDetail(runId: String): RedteamBatchRunDetail
}
