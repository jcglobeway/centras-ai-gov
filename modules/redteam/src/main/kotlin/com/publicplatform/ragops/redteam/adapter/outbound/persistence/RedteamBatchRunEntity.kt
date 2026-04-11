package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import com.publicplatform.ragops.redteam.domain.BatchRunStatus
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "redteam_batch_runs")
class RedteamBatchRunEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "organization_id", nullable = false) val organizationId: String,
    @Column(name = "triggered_by", nullable = false) val triggeredBy: String,
    @Column(name = "status", nullable = false) val status: String,
    @Column(name = "total_cases", nullable = false) val totalCases: Int,
    @Column(name = "pass_count", nullable = false) val passCount: Int,
    @Column(name = "fail_count", nullable = false) val failCount: Int,
    @Column(name = "pass_rate", nullable = false) val passRate: Double,
    @Column(name = "started_at", nullable = false) val startedAt: Instant,
    @Column(name = "completed_at") val completedAt: Instant?,
)

fun RedteamBatchRunEntity.toSummary(): RedteamBatchRunSummary =
    RedteamBatchRunSummary(
        id = id,
        organizationId = organizationId,
        triggeredBy = triggeredBy,
        status = status.toBatchRunStatus(),
        totalCases = totalCases,
        passCount = passCount,
        failCount = failCount,
        passRate = passRate,
        startedAt = startedAt,
        completedAt = completedAt,
    )

fun RedteamBatchRunSummary.toEntity(): RedteamBatchRunEntity =
    RedteamBatchRunEntity(
        id = id,
        organizationId = organizationId,
        triggeredBy = triggeredBy,
        status = status.name.lowercase(),
        totalCases = totalCases,
        passCount = passCount,
        failCount = failCount,
        passRate = passRate,
        startedAt = startedAt,
        completedAt = completedAt,
    )

private fun String.toBatchRunStatus(): BatchRunStatus = when (this) {
    "pending" -> BatchRunStatus.PENDING
    "running" -> BatchRunStatus.RUNNING
    "completed" -> BatchRunStatus.COMPLETED
    "failed" -> BatchRunStatus.FAILED
    else -> BatchRunStatus.PENDING
}
