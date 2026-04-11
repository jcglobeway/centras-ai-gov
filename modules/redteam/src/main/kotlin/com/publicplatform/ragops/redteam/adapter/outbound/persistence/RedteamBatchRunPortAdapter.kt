package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import com.publicplatform.ragops.redteam.application.port.out.LoadRedteamBatchRunPort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamBatchRunPort
import com.publicplatform.ragops.redteam.domain.BatchRunStatus
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary
import com.publicplatform.ragops.redteam.domain.RedteamCaseResultSummary
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

open class RedteamBatchRunPortAdapter(
    private val jpaRunRepository: JpaRedteamBatchRunRepository,
    private val jpaResultRepository: JpaRedteamCaseResultRepository,
) : LoadRedteamBatchRunPort, SaveRedteamBatchRunPort {

    override fun findById(id: String): RedteamBatchRunSummary? =
        jpaRunRepository.findById(id).orElse(null)?.toSummary()

    override fun findAllByOrganizationId(organizationId: String?): List<RedteamBatchRunSummary> =
        if (organizationId.isNullOrBlank()) {
            jpaRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, 20))
        } else {
            jpaRunRepository.findAllByOrganizationIdOrderByStartedAtDesc(organizationId, PageRequest.of(0, 20))
        }.map { it.toSummary() }

    override fun findResultsByBatchRunId(batchRunId: String): List<RedteamCaseResultSummary> =
        jpaResultRepository.findAllByBatchRunIdOrderByJudgmentAsc(batchRunId).map { it.toSummary() }

    @Transactional
    override fun save(run: RedteamBatchRunSummary): RedteamBatchRunSummary =
        jpaRunRepository.save(run.toEntity()).toSummary()

    @Transactional
    override fun updateResult(
        runId: String,
        passCount: Int,
        failCount: Int,
        passRate: Double,
        completedAt: Instant,
        status: BatchRunStatus,
    ) {
        val entity = jpaRunRepository.findById(runId).orElseThrow {
            IllegalArgumentException("BatchRun not found: $runId")
        }
        val updated = RedteamBatchRunEntity(
            id = entity.id,
            organizationId = entity.organizationId,
            triggeredBy = entity.triggeredBy,
            status = status.name.lowercase(),
            totalCases = entity.totalCases,
            passCount = passCount,
            failCount = failCount,
            passRate = passRate,
            startedAt = entity.startedAt,
            completedAt = completedAt,
        )
        jpaRunRepository.save(updated)
    }
}
