package com.publicplatform.ragops.adminapi.ingestion

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import com.publicplatform.ragops.ingestionops.CrawlCollectionMode
import com.publicplatform.ragops.ingestionops.CrawlRenderMode
import com.publicplatform.ragops.ingestionops.CrawlSourceReader
import com.publicplatform.ragops.ingestionops.CrawlSourceType
import com.publicplatform.ragops.ingestionops.CrawlSourceWriter
import com.publicplatform.ragops.ingestionops.CreateCrawlSourceCommand
import com.publicplatform.ragops.ingestionops.IngestionJobReader
import com.publicplatform.ragops.ingestionops.IngestionJobStage
import com.publicplatform.ragops.ingestionops.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.IngestionJobType
import com.publicplatform.ragops.ingestionops.IngestionJobWriter
import com.publicplatform.ragops.ingestionops.IngestionScope
import com.publicplatform.ragops.ingestionops.InvalidIngestionJobTransitionException
import com.publicplatform.ragops.ingestionops.RequestIngestionJobCommand
import com.publicplatform.ragops.ingestionops.TransitionIngestionJobCommand
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

@RestController
@RequestMapping("/admin")
class IngestionCommandController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val crawlSourceReader: CrawlSourceReader,
    private val crawlSourceWriter: CrawlSourceWriter,
    private val ingestionJobReader: IngestionJobReader,
    private val ingestionJobWriter: IngestionJobWriter,
) {
    @PostMapping("/crawl-sources")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCrawlSource(
        @Valid @RequestBody request: CreateCrawlSourceRequest,
        servletRequest: HttpServletRequest,
    ): CrawlSourceCreateResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        ensureOrganizationScope(session.toScope(), request.organizationId)

        val createdSource =
            crawlSourceWriter.createSource(
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

        return CrawlSourceCreateResponse(
            crawlSourceId = createdSource.id,
            saved = true,
        )
    }

    @PostMapping("/crawl-sources/{id}/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun runCrawlSource(
        @PathVariable id: String,
        servletRequest: HttpServletRequest,
    ): IngestionJobCreateResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()
        ensureReadableSource(scope, id)

        val createdJob =
            ingestionJobWriter.requestJob(
                RequestIngestionJobCommand(
                    crawlSourceId = id,
                    requestedBy = session.user.id,
                    triggerType = "manual",
                    jobType = IngestionJobType.CRAWL,
                ),
            )

        return IngestionJobCreateResponse(
            jobId = createdJob.id,
            status = createdJob.status.toApiValue(),
        )
    }

    @PostMapping("/ingestion-jobs/{id}/status")
    fun transitionIngestionJob(
        @PathVariable id: String,
        @Valid @RequestBody request: TransitionIngestionJobRequest,
        servletRequest: HttpServletRequest,
    ): IngestionJobTransitionResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()
        ensureReadableJob(scope, id)

        val updatedJob =
            try {
                ingestionJobWriter.transitionJob(
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
            jobId = updatedJob.id,
            jobStatus = updatedJob.status.toApiValue(),
            jobStage = updatedJob.stage.toApiValue(),
        )
    }

    private fun ensureOrganizationScope(
        scope: IngestionScope,
        requestedOrganizationId: String,
    ) {
        // 권한 미들웨어 전까지는 조직 범위를 명시적으로 막아 두어야 스텁 환경이 퍼지지 않는다.
        if (!scope.globalAccess && requestedOrganizationId !in scope.organizationIds) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "해당 기관 범위에 접근할 수 없습니다.")
        }
    }

    private fun ensureReadableSource(
        scope: IngestionScope,
        crawlSourceId: String,
    ) {
        if (crawlSourceReader.listSources(scope).none { it.id == crawlSourceId }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 crawl source 를 찾을 수 없습니다.")
        }
    }

    private fun ensureReadableJob(
        scope: IngestionScope,
        jobId: String,
    ) {
        if (ingestionJobReader.listJobs(scope).none { it.id == jobId }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 ingestion job 을 찾을 수 없습니다.")
        }
    }
}

data class CreateCrawlSourceRequest(
    @field:NotBlank
    val organizationId: String,
    @field:NotBlank
    val serviceId: String,
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val sourceType: String,
    @field:NotBlank
    val sourceUri: String,
    @field:NotBlank
    val renderMode: String,
    @field:NotBlank
    val collectionMode: String,
    @field:NotBlank
    val scheduleExpr: String,
)

data class CrawlSourceCreateResponse(
    val crawlSourceId: String,
    val saved: Boolean,
)

data class IngestionJobCreateResponse(
    val jobId: String,
    val status: String,
)

data class TransitionIngestionJobRequest(
    @field:NotBlank
    val jobStatus: String,
    @field:NotBlank
    val jobStage: String,
    val errorCode: String? = null,
)

data class IngestionJobTransitionResponse(
    val jobId: String,
    val jobStatus: String,
    val jobStage: String,
)

private fun AdminSessionSnapshot.toScope(): IngestionScope =
    IngestionScope(
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
