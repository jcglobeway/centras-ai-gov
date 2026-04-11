package com.publicplatform.ragops.adminapi.redteam.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationException
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.identityaccess.domain.AuthorizationFailureReason
import com.publicplatform.ragops.redteam.application.port.`in`.ListRedteamBatchRunsUseCase
import com.publicplatform.ragops.redteam.application.port.`in`.RunRedteamBatchUseCase
import com.publicplatform.ragops.redteam.domain.RedteamBatchRunSummary
import com.publicplatform.ragops.redteam.domain.RedteamCaseResultSummary
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/admin/redteam")
class RedteamBatchRunController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val runRedteamBatchUseCase: RunRedteamBatchUseCase,
    private val listRedteamBatchRunsUseCase: ListRedteamBatchRunsUseCase,
) {

    @PostMapping("/batch-runs")
    @ResponseStatus(HttpStatus.CREATED)
    fun runBatch(
        @Valid @RequestBody request: RunBatchRequest,
        servletRequest: HttpServletRequest,
    ): RedteamBatchRunResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "redteam.batch.run")

        return try {
            runRedteamBatchUseCase.runBatch(
                organizationId = request.organizationId,
                triggeredBy = session.user.id,
            ).toResponse()
        } catch (e: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        }
    }

    @GetMapping("/batch-runs")
    fun listRuns(
        @RequestParam("organizationId", required = false) organizationId: String?,
        servletRequest: HttpServletRequest,
    ): RedteamBatchRunListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "redteam.batch.read")

        val runs = listRedteamBatchRunsUseCase.listRuns(organizationId)
        return RedteamBatchRunListResponse(runs = runs.map { it.toResponse() }, total = runs.size)
    }

    @GetMapping("/batch-runs/{id}")
    fun getRunDetail(
        @PathVariable id: String,
        servletRequest: HttpServletRequest,
    ): RedteamBatchRunDetailResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "redteam.batch.read")

        val detail = try {
            listRedteamBatchRunsUseCase.getRunDetail(id)
        } catch (e: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return RedteamBatchRunDetailResponse(
            run = detail.run.toResponse(),
            results = detail.results.map { it.toResultResponse() },
        )
    }

    private fun requireAuthorized(session: AdminSessionSnapshot, actionCode: String) {
        try {
            adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode))
        } catch (e: AdminAuthorizationException) {
            val status = when (e.reason) {
                AuthorizationFailureReason.ACTION_FORBIDDEN -> HttpStatus.FORBIDDEN
                AuthorizationFailureReason.SCOPE_FORBIDDEN -> HttpStatus.FORBIDDEN
            }
            throw ResponseStatusException(status, e.message, e)
        }
    }
}

data class RunBatchRequest(
    @field:NotBlank val organizationId: String,
)

data class RedteamBatchRunResponse(
    val id: String,
    val organizationId: String,
    val triggeredBy: String,
    val status: String,
    val totalCases: Int,
    val passCount: Int,
    val failCount: Int,
    val passRate: Double,
    val startedAt: Instant,
    val completedAt: Instant?,
)

data class RedteamBatchRunListResponse(
    val runs: List<RedteamBatchRunResponse>,
    val total: Int,
)

data class RedteamCaseResultResponse(
    val id: String,
    val batchRunId: String,
    val caseId: String,
    val queryText: String,
    val responseText: String,
    val answerStatus: String,
    val judgment: String,
    val judgmentDetail: String?,
    val executedAt: Instant,
)

data class RedteamBatchRunDetailResponse(
    val run: RedteamBatchRunResponse,
    val results: List<RedteamCaseResultResponse>,
)

fun RedteamBatchRunSummary.toResponse() = RedteamBatchRunResponse(
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

fun RedteamCaseResultSummary.toResultResponse() = RedteamCaseResultResponse(
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
