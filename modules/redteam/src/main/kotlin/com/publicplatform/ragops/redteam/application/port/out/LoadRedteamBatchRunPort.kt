package com.publicplatform.ragops.redteam.application.port.out

import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary
import com.publicplatform.ragops.redteam.domain.RedteamCaseResultSummary

interface LoadRedteamBatchRunPort {
    fun findById(id: String): RedteamBatchRunSummary?
    fun findAllByOrganizationId(organizationId: String?): List<RedteamBatchRunSummary>
    fun findResultsByBatchRunId(batchRunId: String): List<RedteamCaseResultSummary>
}
