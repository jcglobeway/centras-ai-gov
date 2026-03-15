package com.publicplatform.ragops.ingestionops

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "crawl_sources")
class CrawlSourceEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "organization_id", nullable = false)
    val organizationId: String,

    @Column(name = "service_id", nullable = false)
    val serviceId: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "source_type", nullable = false)
    val sourceType: String,

    @Column(name = "source_uri", nullable = false, columnDefinition = "TEXT")
    val sourceUri: String,

    @Column(name = "collection_mode", nullable = false)
    val collectionMode: String,

    @Column(name = "render_mode", nullable = false)
    val renderMode: String,

    @Column(name = "schedule_expr", nullable = false)
    val scheduleExpr: String,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "status", nullable = false)
    val status: String,

    @Column(name = "last_crawled_at")
    val lastCrawledAt: Instant?,

    @Column(name = "last_succeeded_at")
    val lastSucceededAt: Instant?,

    @Column(name = "last_job_id")
    val lastJobId: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

fun CrawlSourceEntity.toSummary(): CrawlSourceSummary =
    CrawlSourceSummary(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        name = name,
        sourceType = sourceType.toSourceType(),
        sourceUri = sourceUri,
        renderMode = renderMode.toRenderMode(),
        collectionMode = collectionMode.toCollectionMode(),
        schedule = scheduleExpr,
        status = status.toSourceStatus(),
        lastSucceededAt = lastSucceededAt,
        lastJobId = lastJobId,
    )

private fun String.toSourceType(): CrawlSourceType =
    when (this) {
        "website" -> CrawlSourceType.WEBSITE
        "sitemap" -> CrawlSourceType.SITEMAP
        "file_drop" -> CrawlSourceType.FILE_DROP
        else -> CrawlSourceType.WEBSITE
    }

private fun String.toRenderMode(): CrawlRenderMode =
    when (this) {
        "http_static" -> CrawlRenderMode.HTTP_STATIC
        "browser_playwright" -> CrawlRenderMode.BROWSER_PLAYWRIGHT
        "browser_lightpanda" -> CrawlRenderMode.BROWSER_LIGHTPANDA
        else -> CrawlRenderMode.HTTP_STATIC
    }

private fun String.toCollectionMode(): CrawlCollectionMode =
    when (this) {
        "full" -> CrawlCollectionMode.FULL
        "incremental" -> CrawlCollectionMode.INCREMENTAL
        else -> CrawlCollectionMode.INCREMENTAL
    }

private fun String.toSourceStatus(): CrawlSourceStatus =
    when (this) {
        "active" -> CrawlSourceStatus.ACTIVE
        "paused" -> CrawlSourceStatus.PAUSED
        "error" -> CrawlSourceStatus.ERROR
        else -> CrawlSourceStatus.ACTIVE
    }

fun CrawlSourceSummary.toEntity(): CrawlSourceEntity =
    CrawlSourceEntity(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        name = name,
        sourceType = sourceType.name.lowercase(),
        sourceUri = sourceUri,
        collectionMode = collectionMode.name.lowercase(),
        renderMode = renderMode.name.lowercase(),
        scheduleExpr = schedule,
        isActive = status == CrawlSourceStatus.ACTIVE,
        status = status.name.lowercase(),
        lastCrawledAt = null,
        lastSucceededAt = lastSucceededAt,
        lastJobId = lastJobId,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
