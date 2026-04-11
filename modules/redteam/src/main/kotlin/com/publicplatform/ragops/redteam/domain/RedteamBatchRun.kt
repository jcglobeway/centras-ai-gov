package com.publicplatform.ragops.redteam.domain

import java.time.Instant

enum class BatchRunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
}

data class RedteamBatchRun(
    val id: String,
    val organizationId: String,
    val triggeredBy: String,
    val status: BatchRunStatus,
    val totalCases: Int,
    val passCount: Int,
    val failCount: Int,
    val passRate: Double,
    val startedAt: Instant,
    val completedAt: Instant?,
)

data class RedteamBatchRunSummary(
    val id: String,
    val organizationId: String,
    val triggeredBy: String,
    val status: BatchRunStatus,
    val totalCases: Int,
    val passCount: Int,
    val failCount: Int,
    val passRate: Double,
    val startedAt: Instant,
    val completedAt: Instant?,
)

data class RedteamBatchRunDetail(
    val run: RedteamBatchRunSummary,
    val results: List<RedteamCaseResultSummary>,
)
