package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import com.publicplatform.ragops.redteam.domain.RedteamCaseResultSummary
import com.publicplatform.ragops.redteam.domain.RedteamJudgment
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "redteam_case_results")
class RedteamCaseResultEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "batch_run_id", nullable = false) val batchRunId: String,
    @Column(name = "case_id", nullable = false) val caseId: String,
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT") val queryText: String,
    @Column(name = "response_text", nullable = false, columnDefinition = "TEXT") val responseText: String,
    @Column(name = "answer_status", nullable = false) val answerStatus: String,
    @Column(name = "judgment", nullable = false) val judgment: String,
    @Column(name = "judgment_detail", columnDefinition = "TEXT") val judgmentDetail: String?,
    @Column(name = "executed_at", nullable = false) val executedAt: Instant,
)

fun RedteamCaseResultEntity.toSummary(): RedteamCaseResultSummary =
    RedteamCaseResultSummary(
        id = id,
        batchRunId = batchRunId,
        caseId = caseId,
        queryText = queryText,
        responseText = responseText,
        answerStatus = answerStatus,
        judgment = judgment.toRedteamJudgment(),
        judgmentDetail = judgmentDetail,
        executedAt = executedAt,
    )

fun RedteamCaseResultSummary.toEntity(): RedteamCaseResultEntity =
    RedteamCaseResultEntity(
        id = id,
        batchRunId = batchRunId,
        caseId = caseId,
        queryText = queryText,
        responseText = responseText,
        answerStatus = answerStatus,
        judgment = judgment.name.lowercase(),
        judgmentDetail = judgmentDetail,
        executedAt = executedAt,
    )

private fun String.toRedteamJudgment(): RedteamJudgment = when (this) {
    "pass" -> RedteamJudgment.PASS
    "fail" -> RedteamJudgment.FAIL
    "skip" -> RedteamJudgment.SKIP
    else -> RedteamJudgment.SKIP
}
