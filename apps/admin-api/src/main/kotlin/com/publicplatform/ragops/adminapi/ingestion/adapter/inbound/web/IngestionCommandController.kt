package com.publicplatform.ragops.adminapi.ingestion.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationException
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.identityaccess.domain.AuthorizationFailureReason
import com.publicplatform.ragops.ingestionops.application.port.`in`.CreateCrawlSourceUseCase
import com.publicplatform.ragops.ingestionops.application.port.`in`.ListIngestionUseCase
import com.publicplatform.ragops.ingestionops.application.port.`in`.RunIngestionJobUseCase
import com.publicplatform.ragops.ingestionops.application.port.`in`.TransitionJobUseCase
import com.publicplatform.ragops.ingestionops.domain.CrawlCollectionMode
import com.publicplatform.ragops.ingestionops.domain.CrawlRenderMode
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceType
import com.publicplatform.ragops.ingestionops.domain.CreateCrawlSourceCommand
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStage
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.domain.IngestionJobType
import com.publicplatform.ragops.ingestionops.domain.IngestionScope
import com.publicplatform.ragops.ingestionops.domain.InvalidIngestionJobTransitionException
import com.publicplatform.ragops.ingestionops.domain.RequestIngestionJobCommand
import com.publicplatform.ragops.ingestionops.domain.TransitionIngestionJobCommand
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * 인제스션 명령 HTTP 인바운드 어댑터.
 *
 * 크롤 소스 생성, 잡 실행 요청, 잡 상태 전이를 각 UseCase에 위임한다.
 * 권한 검사는 AdminAuthorizationPolicy에 위임하며 비즈니스 로직을 포함하지 않는다.
 */
@RestController
@RequestMapping("/admin")
class IngestionCommandController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val createCrawlSourceUseCase: CreateCrawlSourceUseCase,
    private val runIngestionJobUseCase: RunIngestionJobUseCase,
    private val listIngestionUseCase: ListIngestionUseCase,
    private val transitionJobUseCase: TransitionJobUseCase,
) {
    @PostMapping("/crawl-sources")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCrawlSource(
        @Valid @RequestBody request: CreateCrawlSourceRequest,
        servletRequest: HttpServletRequest,
    ): CrawlSourceCreateResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "crawl_source.write", request.organizationId)

        val created = createCrawlSourceUseCase.execute(
            CreateCrawlSourceCommand(
                organizationId = request.organizationId,
                serviceId = request.serviceId,
                name = request.name,
                sourceType = request.sourceType.toSourceType(),
                sourceUri = request.sourceUri,
                renderMode = request.renderMode.toRenderMode(),
                collectionMode = request.collectionMode.toCollectionMode(),
                schedule = request.scheduleExpr,
                requestedBy = session.user.id,
            ),
        )

        return CrawlSourceCreateResponse(crawlSourceId = created.id, saved = true)
    }

    @PostMapping("/crawl-sources/{id}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun runCrawlSource(
        @PathVariable id: String,
        servletRequest: HttpServletRequest,
    ): IngestionJobCreateResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()
        requireAuthorized(session, "crawl_source.write")
        ensureReadableSource(scope, id)

        val created = runIngestionJobUseCase.execute(
            RequestIngestionJobCommand(
                crawlSourceId = id,
                requestedBy = session.user.id,
                triggerType = "manual",
                jobType = IngestionJobType.CRAWL,
            ),
        )

        return IngestionJobCreateResponse(jobId = created.id, status = created.status.toApiValue())
    }

    @PostMapping("/ingestion-jobs/{id}/status")
    fun transitionIngestionJob(
        @PathVariable id: String,
        @Valid @RequestBody request: TransitionIngestionJobRequest,
        servletRequest: HttpServletRequest,
    ): IngestionJobTransitionResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()
        requireAuthorized(session, "crawl_source.write")
        ensureReadableJob(scope, id)

        val updated = try {
            transitionJobUseCase.execute(
                TransitionIngestionJobCommand(
                    jobId = id,
                    nextStatus = request.jobStatus.toJobStatus(),
                    nextStage = request.jobStage.toJobStage(),
                    updatedBy = session.user.id,
                    errorCode = request.errorCode,
                ),
            )
        } catch (exception: InvalidIngestionJobTransitionException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, exception.message, exception)
        }

        return IngestionJobTransitionResponse(
            jobId = updated.id,
            jobStatus = updated.status.toApiValue(),
            jobStage = updated.stage.toApiValue(),
        )
    }

    private fun requireAuthorized(session: AdminSessionSnapshot, actionCode: String, organizationId: String? = null) {
        try {
            adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode, organizationId))
        } catch (exception: AdminAuthorizationException) {
            val status = when (exception.reason) {
                AuthorizationFailureReason.ACTION_FORBIDDEN -> HttpStatus.FORBIDDEN
                AuthorizationFailureReason.SCOPE_FORBIDDEN -> HttpStatus.FORBIDDEN
            }
            throw ResponseStatusException(status, exception.message, exception)
        }
    }

    private fun ensureReadableSource(scope: IngestionScope, id: String) {
        if (listIngestionUseCase.listSources(scope).none { it.id == id }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 crawl source 를 찾을 수 없습니다.")
        }
    }

    private fun ensureReadableJob(scope: IngestionScope, id: String) {
        if (listIngestionUseCase.listJobs(scope).none { it.id == id }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 ingestion job 을 찾을 수 없습니다.")
        }
    }
}

data class CreateCrawlSourceRequest(
    @field:NotBlank val organizationId: String,
    @field:NotBlank val serviceId: String,
    @field:NotBlank val name: String,
    @field:NotBlank val sourceType: String,
    @field:NotBlank val sourceUri: String,
    @field:NotBlank val renderMode: String,
    @field:NotBlank val collectionMode: String,
    @field:NotBlank val scheduleExpr: String,
)

data class CrawlSourceCreateResponse(val crawlSourceId: String, val saved: Boolean)
data class IngestionJobCreateResponse(val jobId: String, val status: String)
data class TransitionIngestionJobRequest(
    @field:NotBlank val jobStatus: String,
    @field:NotBlank val jobStage: String,
    val errorCode: String? = null,
)
data class IngestionJobTransitionResponse(val jobId: String, val jobStatus: String, val jobStage: String)

private fun AdminSessionSnapshot.toScope() = IngestionScope(
    organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
    globalAccess = roleAssignments.any { it.organizationId == null },
)

private fun String.toSourceType(): CrawlSourceType =
    when (this) {
        "website" -> CrawlSourceType.WEBSITE
        "sitemap" -> CrawlSourceType.SITEMAP
        "file_drop" -> CrawlSourceType.FILE_DROP
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 source_type 입니다: $this")
    }

private fun String.toRenderMode(): CrawlRenderMode =
    when (this) {
        "http_static" -> CrawlRenderMode.HTTP_STATIC
        "browser_playwright" -> CrawlRenderMode.BROWSER_PLAYWRIGHT
        "browser_lightpanda" -> CrawlRenderMode.BROWSER_LIGHTPANDA
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 render_mode 입니다: $this")
    }

private fun String.toCollectionMode(): CrawlCollectionMode =
    when (this) {
        "full" -> CrawlCollectionMode.FULL
        "incremental" -> CrawlCollectionMode.INCREMENTAL
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 collection_mode 입니다: $this")
    }

private fun String.toJobStatus(): IngestionJobStatus =
    when (this) {
        "queued" -> IngestionJobStatus.QUEUED
        "running" -> IngestionJobStatus.RUNNING
        "succeeded" -> IngestionJobStatus.SUCCEEDED
        "partial_success" -> IngestionJobStatus.PARTIAL_SUCCESS
        "failed" -> IngestionJobStatus.FAILED
        "cancelled" -> IngestionJobStatus.CANCELLED
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 job_status 입니다: $this")
    }

private fun String.toJobStage(): IngestionJobStage =
    when (this) {
        "fetch" -> IngestionJobStage.FETCH
        "extract" -> IngestionJobStage.EXTRACT
        "normalize" -> IngestionJobStage.NORMALIZE
        "chunk" -> IngestionJobStage.CHUNK
        "embed" -> IngestionJobStage.EMBED
        "index" -> IngestionJobStage.INDEX
        "complete" -> IngestionJobStage.COMPLETE
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 job_stage 입니다: $this")
    }

private fun IngestionJobStatus.toApiValue(): String =
    when (this) {
        IngestionJobStatus.QUEUED -> "queued"
        IngestionJobStatus.RUNNING -> "running"
        IngestionJobStatus.SUCCEEDED -> "succeeded"
        IngestionJobStatus.PARTIAL_SUCCESS -> "partial_success"
        IngestionJobStatus.FAILED -> "failed"
        IngestionJobStatus.CANCELLED -> "cancelled"
    }

private fun IngestionJobStage.toApiValue(): String =
    when (this) {
        IngestionJobStage.FETCH -> "fetch"
        IngestionJobStage.EXTRACT -> "extract"
        IngestionJobStage.NORMALIZE -> "normalize"
        IngestionJobStage.CHUNK -> "chunk"
        IngestionJobStage.EMBED -> "embed"
        IngestionJobStage.INDEX -> "index"
        IngestionJobStage.COMPLETE -> "complete"
    }
