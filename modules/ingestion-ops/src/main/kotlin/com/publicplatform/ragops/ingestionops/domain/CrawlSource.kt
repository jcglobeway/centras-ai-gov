/**
 * 크롤 소스 도메인 모델 — 지식 수집 대상 정의.
 *
 * WEBSITE/SITEMAP/FILE_DROP 유형별로 렌더 방식(정적/Playwright)과 수집 모드(전체/증분)를 설정한다.
 * 소스 상태가 ERROR이면 다음 스케줄 실행 전에 운영자가 원인을 확인해야 한다.
 */
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
    val collectionName: String?,
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
    val collectionName: String? = null,
)
