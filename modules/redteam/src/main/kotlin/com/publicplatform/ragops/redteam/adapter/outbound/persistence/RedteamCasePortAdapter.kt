package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import com.publicplatform.ragops.redteam.application.port.out.LoadRedteamCasePort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamCasePort
import com.publicplatform.ragops.redteam.domain.RedteamCaseSummary
import org.springframework.transaction.annotation.Transactional

open class RedteamCasePortAdapter(
    private val jpaRepository: JpaRedteamCaseRepository,
) : LoadRedteamCasePort, SaveRedteamCasePort {

    override fun findById(id: String): RedteamCaseSummary? =
        jpaRepository.findById(id).orElse(null)?.toSummary()

    override fun findAll(): List<RedteamCaseSummary> =
        jpaRepository.findAllByOrderByCreatedAtDesc().map { it.toSummary() }

    override fun findAllActive(): List<RedteamCaseSummary> =
        jpaRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().map { it.toSummary() }

    @Transactional
    override fun save(case: RedteamCaseSummary): RedteamCaseSummary =
        jpaRepository.save(case.toEntity()).toSummary()

    @Transactional
    override fun update(case: RedteamCaseSummary): RedteamCaseSummary =
        jpaRepository.save(case.toEntity()).toSummary()

    @Transactional
    override fun delete(id: String) =
        jpaRepository.deleteById(id)
}
