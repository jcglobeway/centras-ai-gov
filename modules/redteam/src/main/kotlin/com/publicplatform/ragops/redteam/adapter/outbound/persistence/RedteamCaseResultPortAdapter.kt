package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamCaseResultPort
import com.publicplatform.ragops.redteam.domain.RedteamCaseResultSummary
import org.springframework.transaction.annotation.Transactional

open class RedteamCaseResultPortAdapter(
    private val jpaRepository: JpaRedteamCaseResultRepository,
) : SaveRedteamCaseResultPort {

    @Transactional
    override fun save(result: RedteamCaseResultSummary): RedteamCaseResultSummary =
        jpaRepository.save(result.toEntity()).toSummary()
}
