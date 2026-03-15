package com.publicplatform.ragops.adminapi.ingestion

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.ingestionops.CrawlSourceReader
import com.publicplatform.ragops.ingestionops.CrawlSourceStatus
import com.publicplatform.ragops.ingestionops.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.CrawlSourceType
import com.publicplatform.ragops.ingestionops.IngestionJobReader
import com.publicplatform.ragops.ingestionops.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.IngestionJobStep
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
    val name: String,
    val sourceType: String,
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
    val crawlSourceId: String,
    val step: String,
    val status: String,
    val triggerType: String,
    val requestedAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
)

private fun CrawlSourceSummary.toResponse(): CrawlSourceResponse =
    CrawlSourceResponse(
        id = id,
        organizationId = organizationId,
        name = name,
        sourceType = sourceType.toApiValue(),
        schedule = schedule,
        status = status.toApiValue(),
        lastSucceededAt = lastSucceededAt,
        lastJobId = lastJobId,
    )

private fun IngestionJobSummary.toResponse(): IngestionJobResponse =
    IngestionJobResponse(
        id = id,
        organizationId = organizationId,
        crawlSourceId = crawlSourceId,
        step = step.toApiValue(),
        status = status.toApiValue(),
        triggerType = triggerType,
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

private fun CrawlSourceStatus.toApiValue(): String =
    when (this) {
        CrawlSourceStatus.ACTIVE -> "active"
        CrawlSourceStatus.PAUSED -> "paused"
        CrawlSourceStatus.ERROR -> "error"
    }

private fun IngestionJobStep.toApiValue(): String =
    when (this) {
        IngestionJobStep.CRAWL -> "crawl"
        IngestionJobStep.PARSE -> "parse"
        IngestionJobStep.CHUNK -> "chunk"
        IngestionJobStep.EMBED -> "embed"
        IngestionJobStep.INDEX -> "index"
    }

private fun IngestionJobStatus.toApiValue(): String =
    when (this) {
        IngestionJobStatus.QUEUED -> "queued"
        IngestionJobStatus.RUNNING -> "running"
        IngestionJobStatus.SUCCEEDED -> "succeeded"
        IngestionJobStatus.FAILED -> "failed"
    }
