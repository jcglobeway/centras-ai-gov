package com.publicplatform.ragops.redteam.application.service

import com.publicplatform.ragops.chatruntime.application.port.out.RagOrchestrationPort
import com.publicplatform.ragops.redteam.application.port.`in`.RunRedteamBatchUseCase
import com.publicplatform.ragops.redteam.application.port.out.LoadRedteamCasePort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamBatchRunPort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamCaseResultPort
import com.publicplatform.ragops.redteam.domain.BatchRunStatus
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary
import com.publicplatform.ragops.redteam.domain.RedteamCaseResultSummary
import com.publicplatform.ragops.redteam.domain.RedteamJudge
import com.publicplatform.ragops.redteam.domain.RedteamJudgment
import java.time.Instant
import java.util.UUID

class RunRedteamBatchService(
    private val loadRedteamCasePort: LoadRedteamCasePort,
    private val saveRedteamBatchRunPort: SaveRedteamBatchRunPort,
    private val saveRedteamCaseResultPort: SaveRedteamCaseResultPort,
    private val ragOrchestrationPort: RagOrchestrationPort,
) : RunRedteamBatchUseCase {

    override fun runBatch(organizationId: String, triggeredBy: String): RedteamBatchRunSummary {
        val activeCases = loadRedteamCasePort.findAllActive()
        if (activeCases.isEmpty()) {
            throw IllegalStateException("활성 케이스가 없습니다. 케이스를 먼저 등록하세요.")
        }

        val batchRunId = "rtbr_${UUID.randomUUID().toString().substring(0, 8)}"
        val startedAt = Instant.now()

        val runSummary = saveRedteamBatchRunPort.save(
            RedteamBatchRunSummary(
                id = batchRunId,
                organizationId = organizationId,
                triggeredBy = triggeredBy,
                status = BatchRunStatus.RUNNING,
                totalCases = activeCases.size,
                passCount = 0,
                failCount = 0,
                passRate = 0.0,
                startedAt = startedAt,
                completedAt = null,
            ),
        )

        var passCount = 0
        var failCount = 0

        for (case in activeCases) {
            val (responseText, answerStatus, judgment, detail) = try {
                val result = ragOrchestrationPort.generateAnswer(
                    questionId = "rt_q_${UUID.randomUUID().toString().substring(0, 8)}",
                    questionText = case.queryText,
                    organizationId = organizationId,
                    serviceId = "redteam",
                )
                if (result != null) {
                    val status = result.answerStatus.name.lowercase()
                    val (j, d) = RedteamJudge.judge(case.category, case.expectedBehavior, result.answerText, status)
                    Quad(result.answerText, status, j, d)
                } else {
                    Quad("", "fallback", RedteamJudgment.SKIP, "RAG orchestrator 응답 없음")
                }
            } catch (e: Exception) {
                Quad("", "error", RedteamJudgment.FAIL, "RAG 호출 오류: ${e.message}")
            }

            when (judgment) {
                RedteamJudgment.PASS -> passCount++
                RedteamJudgment.FAIL -> failCount++
                RedteamJudgment.SKIP -> { }
            }

            saveRedteamCaseResultPort.save(
                RedteamCaseResultSummary(
                    id = "rtcr_${UUID.randomUUID().toString().substring(0, 8)}",
                    batchRunId = batchRunId,
                    caseId = case.id,
                    queryText = case.queryText,
                    responseText = responseText,
                    answerStatus = answerStatus,
                    judgment = judgment,
                    judgmentDetail = detail,
                    executedAt = Instant.now(),
                ),
            )
        }

        val completedAt = Instant.now()
        val effectiveTotal = (passCount + failCount).takeIf { it > 0 } ?: 1
        val passRate = passCount.toDouble() / effectiveTotal * 100.0

        saveRedteamBatchRunPort.updateResult(
            runId = batchRunId,
            passCount = passCount,
            failCount = failCount,
            passRate = passRate,
            completedAt = completedAt,
            status = BatchRunStatus.COMPLETED,
        )

        return runSummary.copy(
            passCount = passCount,
            failCount = failCount,
            passRate = passRate,
            completedAt = completedAt,
            status = BatchRunStatus.COMPLETED,
        )
    }

    private data class Quad(
        val responseText: String,
        val answerStatus: String,
        val judgment: RedteamJudgment,
        val detail: String?,
    )
}
