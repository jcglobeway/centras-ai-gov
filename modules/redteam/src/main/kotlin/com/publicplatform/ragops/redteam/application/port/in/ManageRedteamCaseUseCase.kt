package com.publicplatform.ragops.redteam.application.port.`in`

import com.publicplatform.ragops.redteam.domain.CreateRedteamCaseCommand
import com.publicplatform.ragops.redteam.domain.RedteamCaseSummary
import com.publicplatform.ragops.redteam.domain.UpdateRedteamCaseCommand

interface ManageRedteamCaseUseCase {
    fun createCase(command: CreateRedteamCaseCommand): RedteamCaseSummary
    fun updateCase(id: String, command: UpdateRedteamCaseCommand): RedteamCaseSummary
    fun deleteCase(id: String)
    fun listCases(): List<RedteamCaseSummary>
}
