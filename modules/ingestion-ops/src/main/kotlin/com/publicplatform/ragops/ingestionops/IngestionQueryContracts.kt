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
    FAILED,
}

data class IngestionScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)

data class CrawlSourceSummary(
    val id: String,
    val organizationId: String,
    val name: String,
    val sourceType: CrawlSourceType,
    val schedule: String,
    val status: CrawlSourceStatus,
    val lastSucceededAt: Instant?,
    val lastJobId: String?,
)

data class IngestionJobSummary(
    val id: String,
    val organizationId: String,
    val crawlSourceId: String,
    val step: IngestionJobStep,
    val status: IngestionJobStatus,
    val triggerType: String,
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
