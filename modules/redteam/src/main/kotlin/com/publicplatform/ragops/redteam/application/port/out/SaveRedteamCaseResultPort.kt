package com.publicplatform.ragops.redteam.application.port.out

import com.publicplatform.ragops.redteam.domain.RedteamCaseResultSummary

interface SaveRedteamCaseResultPort {
    fun save(result: RedteamCaseResultSummary): RedteamCaseResultSummary
}
