package com.publicplatform.ragops.ingestionops.domain

import java.time.Instant

enum class CrawlSourceType { WEBSITE, SITEMAP, FILE_DROP }
enum class CrawlSourceStatus { ACTIVE, PAUSED, ERROR }
enum class CrawlRenderMode { HTTP_STATIC, BROWSER_PLAYWRIGHT, BROWSER_LIGHTPANDA }
enum class CrawlCollectionMode { FULL, INCREMENTAL }

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

data class CreateCrawlSourceCommand(
    val organizationId: String,
    val serviceId: String,
    val name: String,
    val sourceType: CrawlSourceType,
    val sourceUri: String,
    val renderMode: CrawlRenderMode,
    val collectionMode: CrawlCollectionMode,
    val schedule: String,
    val requestedBy: String,
)
