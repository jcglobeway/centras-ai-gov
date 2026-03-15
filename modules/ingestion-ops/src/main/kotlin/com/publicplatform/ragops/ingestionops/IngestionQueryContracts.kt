package com.publicplatform.ragops.ingestionops

import java.time.Instant

enum class CrawlSourceType {
    WEBSITE,
    SITEMAP,
    FILE_DROP,
}

enum class CrawlSourceStatus {
    ACTIVE,
    PAUSED,
    ERROR,
}

enum class IngestionJobStep {
    CRAWL,
    PARSE,
    CHUNK,
    EMBED,
    INDEX,
}

enum class IngestionJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    PARTIAL_SUCCESS,
    FAILED,
    CANCELLED,
}

enum class IngestionJobType {
    CRAWL,
    PARSE,
    CHUNK,
    EMBED,
    INDEX,
    REINDEX,
}

enum class IngestionJobStage {
    FETCH,
    EXTRACT,
    NORMALIZE,
    CHUNK,
    EMBED,
    INDEX,
    COMPLETE,
}

enum class CrawlRenderMode {
    HTTP_STATIC,
    BROWSER_PLAYWRIGHT,
    BROWSER_LIGHTPANDA,
}

enum class CrawlCollectionMode {
    FULL,
    INCREMENTAL,
}

data class IngestionScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)

data class CrawlSourceSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val name: String,
    val sourceType: CrawlSourceType,
    val sourceUri: String,
    val renderMode: CrawlRenderMode,
    val collectionMode: CrawlCollectionMode,
    val schedule: String,
    val status: CrawlSourceStatus,
    val lastSucceededAt: Instant?,
    val lastJobId: String?,
)

data class IngestionJobSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val crawlSourceId: String,
    val documentId: String?,
    val jobType: IngestionJobType,
    val stage: IngestionJobStage,
    val status: IngestionJobStatus,
    val runnerType: String,
    val triggerType: String,
    val attemptCount: Int,
    val errorCode: String?,
    val requestedAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
)

interface CrawlSourceReader {
    fun listSources(scope: IngestionScope): List<CrawlSourceSummary>
}

interface IngestionJobReader {
    fun listJobs(scope: IngestionScope): List<IngestionJobSummary>
}
