package com.publicplatform.ragops.adminapi.ingestion

import com.publicplatform.ragops.ingestionops.CrawlSourceReader
import com.publicplatform.ragops.ingestionops.CrawlSourceStatus
import com.publicplatform.ragops.ingestionops.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.CrawlSourceType
import com.publicplatform.ragops.ingestionops.IngestionJobReader
import com.publicplatform.ragops.ingestionops.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.IngestionJobStep
import com.publicplatform.ragops.ingestionops.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.IngestionScope
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DevelopmentCrawlSourceReader : CrawlSourceReader {
    private val sources = listOf(
        CrawlSourceSummary(
            id = "crawl_src_001",
            organizationId = "org_seoul_120",
            name = "Seoul Notices",
            sourceType = CrawlSourceType.WEBSITE,
            schedule = "0 */6 * * *",
            status = CrawlSourceStatus.ACTIVE,
            lastSucceededAt = Instant.parse("2026-03-15T01:20:00Z"),
            lastJobId = "ing_job_101",
        ),
        CrawlSourceSummary(
            id = "crawl_src_002",
            organizationId = "org_busan_220",
            name = "Busan FAQ Sitemap",
            sourceType = CrawlSourceType.SITEMAP,
            schedule = "0 */12 * * *",
            status = CrawlSourceStatus.PAUSED,
            lastSucceededAt = Instant.parse("2026-03-14T22:10:00Z"),
            lastJobId = "ing_job_202",
        ),
    )

    override fun listSources(scope: IngestionScope): List<CrawlSourceSummary> =
        sources.filterSourcesByScope(scope)
}

@Service
class DevelopmentIngestionJobReader : IngestionJobReader {
    private val jobs = listOf(
        IngestionJobSummary(
            id = "ing_job_101",
            organizationId = "org_seoul_120",
            crawlSourceId = "crawl_src_001",
            step = IngestionJobStep.INDEX,
            status = IngestionJobStatus.SUCCEEDED,
            triggerType = "scheduled",
            requestedAt = Instant.parse("2026-03-15T01:00:00Z"),
            startedAt = Instant.parse("2026-03-15T01:01:00Z"),
            finishedAt = Instant.parse("2026-03-15T01:20:00Z"),
        ),
        IngestionJobSummary(
            id = "ing_job_202",
            organizationId = "org_busan_220",
            crawlSourceId = "crawl_src_002",
            step = IngestionJobStep.CRAWL,
            status = IngestionJobStatus.FAILED,
            triggerType = "manual",
            requestedAt = Instant.parse("2026-03-14T22:00:00Z"),
            startedAt = Instant.parse("2026-03-14T22:01:00Z"),
            finishedAt = Instant.parse("2026-03-14T22:03:00Z"),
        ),
    )

    override fun listJobs(scope: IngestionScope): List<IngestionJobSummary> =
        jobs.filterJobsByScope(scope)
}

private fun List<CrawlSourceSummary>.filterSourcesByScope(scope: IngestionScope): List<CrawlSourceSummary> =
    if (scope.globalAccess) {
        this
    } else {
        filter { it.organizationId in scope.organizationIds }
    }

private fun List<IngestionJobSummary>.filterJobsByScope(scope: IngestionScope): List<IngestionJobSummary> =
    if (scope.globalAccess) {
        this
    } else {
        filter { it.organizationId in scope.organizationIds }
    }
