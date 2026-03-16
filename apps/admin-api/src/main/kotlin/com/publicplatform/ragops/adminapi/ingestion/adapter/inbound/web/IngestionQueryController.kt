package com.publicplatform.ragops.adminapi.ingestion.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.ingestionops.application.port.`in`.ListIngestionUseCase
import com.publicplatform.ragops.ingestionops.domain.CrawlCollectionMode
import com.publicplatform.ragops.ingestionops.domain.CrawlRenderMode
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceStatus
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceType
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStage
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionJobType
import com.publicplatform.ragops.ingestionops.domain.IngestionScope
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

/**
 * 인제스션 조회 HTTP 인바운드 어댑터.
 *
 * 크롤 소스와 인제스션 잡 목록·단건 조회를 ListIngestionUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class IngestionQueryController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val listIngestionUseCase: ListIngestionUseCase,
) {
    @GetMapping("/crawl-sources")
    fun listCrawlSources(request: HttpServletRequest): CrawlSourceListResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, actionCheck("crawl_source.read"))
        val items = listIngestionUseCase.listSources(session.toScope()).map { it.toResponse() }
        return CrawlSourceListResponse(items = items, total = items.size)
    }

    @GetMapping("/crawl-sources/{id}")
    fun getCrawlSource(@PathVariable id: String, request: HttpServletRequest): CrawlSourceResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, actionCheck("crawl_source.read"))
        return listIngestionUseCase.listSources(session.toScope()).find { it.id == id }?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Crawl source not found: $id")
    }

    @GetMapping("/ingestion-jobs")
    fun listIngestionJobs(request: HttpServletRequest): IngestionJobListResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, actionCheck("ingestion_job.read"))
        val items = listIngestionUseCase.listJobs(session.toScope()).map { it.toResponse() }
        return IngestionJobListResponse(items = items, total = items.size)
    }

    @GetMapping("/ingestion-jobs/{id}")
    fun getIngestionJob(@PathVariable id: String, request: HttpServletRequest): IngestionJobResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, actionCheck("ingestion_job.read"))
        return listIngestionUseCase.listJobs(session.toScope()).find { it.id == id }?.toResponse()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job not found: $id")
    }
}

data class CrawlSourceListResponse(val items: List<CrawlSourceResponse>, val total: Int)

data class CrawlSourceResponse(
    val id: String, val organizationId: String, val serviceId: String, val name: String,
    val sourceType: String, val sourceUri: String, val renderMode: String, val collectionMode: String,
    val schedule: String, val status: String, val lastSucceededAt: Instant?, val lastJobId: String?,
)

data class IngestionJobListResponse(val items: List<IngestionJobResponse>, val total: Int)

data class IngestionJobResponse(
    val id: String, val organizationId: String, val serviceId: String, val crawlSourceId: String,
    val documentId: String?, val jobType: String, val jobStage: String, val status: String,
    val runnerType: String, val triggerType: String, val attemptCount: Int, val errorCode: String?,
    val requestedAt: Instant, val startedAt: Instant?, val finishedAt: Instant?,
)

private fun actionCheck(actionCode: String) = AuthorizationCheck(actionCode = actionCode)

private fun AdminSessionSnapshot.toScope() = IngestionScope(
    organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
    globalAccess = roleAssignments.any { it.organizationId == null },
)

private fun CrawlSourceSummary.toResponse() = CrawlSourceResponse(
    id = id, organizationId = organizationId, serviceId = serviceId, name = name,
    sourceType = sourceType.toApiValue(), sourceUri = sourceUri, renderMode = renderMode.toApiValue(),
    collectionMode = collectionMode.toApiValue(), schedule = schedule, status = status.toApiValue(),
    lastSucceededAt = lastSucceededAt, lastJobId = lastJobId,
)

private fun IngestionJobSummary.toResponse() = IngestionJobResponse(
    id = id, organizationId = organizationId, serviceId = serviceId, crawlSourceId = crawlSourceId,
    documentId = documentId, jobType = jobType.toApiValue(), jobStage = stage.toApiValue(),
    status = status.toApiValue(), runnerType = runnerType, triggerType = triggerType,
    attemptCount = attemptCount, errorCode = errorCode, requestedAt = requestedAt,
    startedAt = startedAt, finishedAt = finishedAt,
)

private fun CrawlSourceType.toApiValue() = when (this) {
    CrawlSourceType.WEBSITE -> "website"; CrawlSourceType.SITEMAP -> "sitemap"
    CrawlSourceType.FILE_DROP -> "file_drop"
}

private fun CrawlRenderMode.toApiValue() = when (this) {
    CrawlRenderMode.HTTP_STATIC -> "http_static"; CrawlRenderMode.BROWSER_PLAYWRIGHT -> "browser_playwright"
    CrawlRenderMode.BROWSER_LIGHTPANDA -> "browser_lightpanda"
}

private fun CrawlCollectionMode.toApiValue() = when (this) {
    CrawlCollectionMode.FULL -> "full"; CrawlCollectionMode.INCREMENTAL -> "incremental"
}

private fun CrawlSourceStatus.toApiValue() = when (this) {
    CrawlSourceStatus.ACTIVE -> "active"; CrawlSourceStatus.PAUSED -> "paused"; CrawlSourceStatus.ERROR -> "error"
}

private fun IngestionJobType.toApiValue() = when (this) {
    IngestionJobType.CRAWL -> "crawl"; IngestionJobType.PARSE -> "parse"; IngestionJobType.CHUNK -> "chunk"
    IngestionJobType.EMBED -> "embed"; IngestionJobType.INDEX -> "index"; IngestionJobType.REINDEX -> "reindex"
}

private fun IngestionJobStage.toApiValue() = when (this) {
    IngestionJobStage.FETCH -> "fetch"; IngestionJobStage.EXTRACT -> "extract"
    IngestionJobStage.NORMALIZE -> "normalize"; IngestionJobStage.CHUNK -> "chunk"
    IngestionJobStage.EMBED -> "embed"; IngestionJobStage.INDEX -> "index"; IngestionJobStage.COMPLETE -> "complete"
}

private fun IngestionJobStatus.toApiValue() = when (this) {
    IngestionJobStatus.QUEUED -> "queued"; IngestionJobStatus.RUNNING -> "running"
    IngestionJobStatus.SUCCEEDED -> "succeeded"; IngestionJobStatus.PARTIAL_SUCCESS -> "partial_success"
    IngestionJobStatus.FAILED -> "failed"; IngestionJobStatus.CANCELLED -> "cancelled"
}
