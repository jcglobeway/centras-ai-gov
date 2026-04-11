package com.publicplatform.ragops.redteam.domain

import java.time.Instant

enum class RedteamJudgment {
    PASS,
    FAIL,
    SKIP,
}

data class RedteamCaseResult(
    val id: String,
    val batchRunId: String,
    val caseId: String,
    val queryText: String,
    val responseText: String,
    val answerStatus: String,
    val judgment: RedteamJudgment,
    val judgmentDetail: String?,
    val executedAt: Instant,
)

data class RedteamCaseResultSummary(
    val id: String,
    val batchRunId: String,
    val caseId: String,
    val queryText: String,
    val responseText: String,
    val answerStatus: String,
    val judgment: RedteamJudgment,
    val judgmentDetail: String?,
    val executedAt: Instant,
)
