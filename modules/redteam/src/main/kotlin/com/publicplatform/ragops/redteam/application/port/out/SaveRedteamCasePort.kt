package com.publicplatform.ragops.redteam.application.port.out

import com.publicplatform.ragops.redteam.domain.RedteamCaseSummary

interface SaveRedteamCasePort {
    fun save(case: RedteamCaseSummary): RedteamCaseSummary
    fun update(case: RedteamCaseSummary): RedteamCaseSummary
    fun delete(id: String)
}
