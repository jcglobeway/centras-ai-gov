package com.publicplatform.ragops.ingestionops

import java.time.Instant

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

data class RequestIngestionJobCommand(
    val crawlSourceId: String,
    val requestedBy: String,
    val triggerType: String,
    val jobType: IngestionJobType,
    val requestedAt: Instant = Instant.now(),
)

data class TransitionIngestionJobCommand(
    val jobId: String,
    val nextStatus: IngestionJobStatus,
    val nextStage: IngestionJobStage,
    val updatedBy: String,
    val errorCode: String? = null,
    val changedAt: Instant = Instant.now(),
)

interface CrawlSourceWriter {
    fun createSource(command: CreateCrawlSourceCommand): CrawlSourceSummary
}

interface IngestionJobWriter {
    fun requestJob(command: RequestIngestionJobCommand): IngestionJobSummary

    fun transitionJob(command: TransitionIngestionJobCommand): IngestionJobSummary
}
