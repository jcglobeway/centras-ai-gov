package com.publicplatform.ragops.redteam.application.service

import com.publicplatform.ragops.redteam.application.port.`in`.ListRedteamBatchRunsUseCase
import com.publicplatform.ragops.redteam.application.port.out.LoadRedteamBatchRunPort
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunDetail
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary

class ListRedteamBatchRunsService(
    private val loadRedteamBatchRunPort: LoadRedteamBatchRunPort,
) : ListRedteamBatchRunsUseCase {

    override fun listRuns(organizationId: String?): List<RedteamBatchRunSummary> =
        loadRedteamBatchRunPort.findAllByOrganizationId(organizationId)

    override fun getRunDetail(runId: String): RedteamBatchRunDetail {
        val run = loadRedteamBatchRunPort.findById(runId)
            ?: throw NoSuchElementException("BatchRun not found: $runId")
        val results = loadRedteamBatchRunPort.findResultsByBatchRunId(runId)
        return RedteamBatchRunDetail(run = run, results = results)
    }
}
