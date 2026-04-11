package com.publicplatform.ragops.redteam.application.port.out

import com.publicplatform.ragops.redteam.domain.RedteamCaseSummary

interface LoadRedteamCasePort {
    fun findById(id: String): RedteamCaseSummary?
    fun findAll(): List<RedteamCaseSummary>
    fun findAllActive(): List<RedteamCaseSummary>
}
