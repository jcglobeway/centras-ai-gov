package com.publicplatform.ragops.adminapi.ingestion

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.ingestionops.CrawlSourceReader
import com.publicplatform.ragops.ingestionops.CrawlCollectionMode
import com.publicplatform.ragops.ingestionops.CrawlRenderMode
import com.publicplatform.ragops.ingestionops.CrawlSourceStatus
import com.publicplatform.ragops.ingestionops.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.CrawlSourceType
import com.publicplatform.ragops.ingestionops.IngestionJobReader
import com.publicplatform.ragops.ingestionops.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.IngestionJobStage
import com.publicplatform.ragops.ingestionops.IngestionJobType
import com.publicplatform.ragops.ingestionops.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.IngestionScope
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin")
class IngestionQueryController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val crawlSourceReader: CrawlSourceReader,
    private val ingestionJobReader: IngestionJobReader,
) {
    @GetMapping("/crawl-sources")
    fun listCrawlSources(request: HttpServletRequest): CrawlSourceListResponse {
        val scope = request.toScope()
        val items = crawlSourceReader.listSources(scope).map { it.toResponse() }
        return CrawlSourceListResponse(items = items, total = items.size)
    }

    @GetMapping("/ingestion-jobs")
    fun listIngestionJobs(request: HttpServletRequest): IngestionJobListResponse {
        val scope = request.toScope()
        val items = ingestionJobReader.listJobs(scope).map { it.toResponse() }
        return IngestionJobListResponse(items = items, total = items.size)
    }

    private fun HttpServletRequest.toScope(): IngestionScope {
        val session = adminRequestSessionResolver.resolve(this)
        val organizationIds = session.roleAssignments.mapNotNull { it.organizationId }.toSet()
        val globalAccess = session.roleAssignments.any { it.organizationId == null }
        return IngestionScope(
            organizationIds = organizationIds,
            globalAccess = globalAccess,
        )
    }
}

data class CrawlSourceListResponse(
    val items: List<CrawlSourceResponse>,
    val total: Int,
)

data class CrawlSourceResponse(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val name: String,
    val sourceType: String,
    val sourceUri: String,
    val renderMode: String,
    val collectionMode: String,
    val schedule: String,
    val status: String,
    val lastSucceededAt: Instant?,
    val lastJobId: String?,
)

data class IngestionJobListResponse(
    val items: List<IngestionJobResponse>,
    val total: Int,
)

data class IngestionJobResponse(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val crawlSourceId: String,
    val documentId: String?,
    val jobType: String,
    val jobStage: String,
    val status: String,
    val runnerType: String,
    val triggerType: String,
    val attemptCount: Int,
    val errorCode: String?,
    val requestedAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
)

private fun CrawlSourceSummary.toResponse(): CrawlSourceResponse =
    CrawlSourceResponse(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        name = name,
        sourceType = sourceType.toApiValue(),
        sourceUri = sourceUri,
        renderMode = renderMode.toApiValue(),
        collectionMode = collectionMode.toApiValue(),
        schedule = schedule,
        status = status.toApiValue(),
        lastSucceededAt = lastSucceededAt,
        lastJobId = lastJobId,
    )

private fun IngestionJobSummary.toResponse(): IngestionJobResponse =
    IngestionJobResponse(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        crawlSourceId = crawlSourceId,
        documentId = documentId,
        jobType = jobType.toApiValue(),
        jobStage = stage.toApiValue(),
        status = status.toApiValue(),
        runnerType = runnerType,
        triggerType = triggerType,
        attemptCount = attemptCount,
        errorCode = errorCode,
        requestedAt = requestedAt,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )

private fun CrawlSourceType.toApiValue(): String =
    when (this) {
        CrawlSourceType.WEBSITE -> "website"
        CrawlSourceType.SITEMAP -> "sitemap"
        CrawlSourceType.FILE_DROP -> "file_drop"
    }

private fun CrawlRenderMode.toApiValue(): String =
    when (this) {
        CrawlRenderMode.HTTP_STATIC -> "http_static"
        CrawlRenderMode.BROWSER_PLAYWRIGHT -> "browser_playwright"
        CrawlRenderMode.BROWSER_LIGHTPANDA -> "browser_lightpanda"
    }

private fun CrawlCollectionMode.toApiValue(): String =
    when (this) {
        CrawlCollectionMode.FULL -> "full"
        CrawlCollectionMode.INCREMENTAL -> "incremental"
    }

private fun CrawlSourceStatus.toApiValue(): String =
    when (this) {
        CrawlSourceStatus.ACTIVE -> "active"
        CrawlSourceStatus.PAUSED -> "paused"
        CrawlSourceStatus.ERROR -> "error"
    }

private fun IngestionJobType.toApiValue(): String =
    when (this) {
        IngestionJobType.CRAWL -> "crawl"
        IngestionJobType.PARSE -> "parse"
        IngestionJobType.CHUNK -> "chunk"
        IngestionJobType.EMBED -> "embed"
        IngestionJobType.INDEX -> "index"
        IngestionJobType.REINDEX -> "reindex"
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

private fun IngestionJobStatus.toApiValue(): String =
    when (this) {
        IngestionJobStatus.QUEUED -> "queued"
        IngestionJobStatus.RUNNING -> "running"
        IngestionJobStatus.SUCCEEDED -> "succeeded"
        IngestionJobStatus.PARTIAL_SUCCESS -> "partial_success"
        IngestionJobStatus.FAILED -> "failed"
        IngestionJobStatus.CANCELLED -> "cancelled"
    }
