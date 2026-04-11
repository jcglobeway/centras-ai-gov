package com.publicplatform.ragops.redteam.application.port.`in`

import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary

interface RunRedteamBatchUseCase {
    fun runBatch(organizationId: String, triggeredBy: String): RedteamBatchRunSummary
}
