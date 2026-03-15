package com.publicplatform.ragops.ingestionops

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "ingestion_jobs")
class IngestionJobEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "organization_id", nullable = false)
    val organizationId: String,

    @Column(name = "service_id", nullable = false)
    val serviceId: String,

    @Column(name = "crawl_source_id", nullable = false)
    val crawlSourceId: String,

    @Column(name = "document_id")
    val documentId: String?,

    @Column(name = "document_version_id")
    val documentVersionId: String?,

    @Column(name = "job_type", nullable = false)
    val jobType: String,

    @Column(name = "job_status", nullable = false)
    val jobStatus: String,

    @Column(name = "job_stage", nullable = false)
    val jobStage: String,

    @Column(name = "trigger_type", nullable = false)
    val triggerType: String,

    @Column(name = "runner_type", nullable = false)
    val runnerType: String,

    @Column(name = "attempt_count", nullable = false)
    val attemptCount: Int = 1,

    @Column(name = "error_code")
    val errorCode: String?,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: Instant,

    @Column(name = "requested_by")
    val requestedBy: String?,

    @Column(name = "started_at")
    val startedAt: Instant?,

    @Column(name = "finished_at")
    val finishedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

fun IngestionJobEntity.toSummary(): IngestionJobSummary =
    IngestionJobSummary(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        crawlSourceId = crawlSourceId,
        documentId = documentId,
        jobType = jobType.toJobType(),
        stage = jobStage.toJobStage(),
        status = jobStatus.toJobStatus(),
        runnerType = runnerType,
        triggerType = triggerType,
        attemptCount = attemptCount,
        errorCode = errorCode,
        requestedAt = requestedAt,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )

fun IngestionJobSummary.toEntity(): IngestionJobEntity =
    IngestionJobEntity(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        crawlSourceId = crawlSourceId,
        documentId = documentId,
        documentVersionId = null,
        jobType = jobType.name.lowercase(),
        jobStatus = status.name.lowercase(),
        jobStage = stage.name.lowercase(),
        triggerType = triggerType,
        runnerType = runnerType,
        attemptCount = attemptCount,
        errorCode = errorCode,
        requestedAt = requestedAt,
        requestedBy = null,
        startedAt = startedAt,
        finishedAt = finishedAt,
        createdAt = requestedAt,
        updatedAt = Instant.now(),
    )

private fun String.toJobType(): IngestionJobType =
    when (this) {
        "crawl" -> IngestionJobType.CRAWL
        "parse" -> IngestionJobType.PARSE
        "chunk" -> IngestionJobType.CHUNK
        "embed" -> IngestionJobType.EMBED
        "index" -> IngestionJobType.INDEX
        "reindex" -> IngestionJobType.REINDEX
        else -> IngestionJobType.CRAWL
    }

private fun String.toJobStage(): IngestionJobStage =
    when (this) {
        "fetch" -> IngestionJobStage.FETCH
        "extract" -> IngestionJobStage.EXTRACT
        "normalize" -> IngestionJobStage.NORMALIZE
        "chunk" -> IngestionJobStage.CHUNK
        "embed" -> IngestionJobStage.EMBED
        "index" -> IngestionJobStage.INDEX
        "complete" -> IngestionJobStage.COMPLETE
        else -> IngestionJobStage.FETCH
    }

private fun String.toJobStatus(): IngestionJobStatus =
    when (this) {
        "queued" -> IngestionJobStatus.QUEUED
        "running" -> IngestionJobStatus.RUNNING
        "succeeded" -> IngestionJobStatus.SUCCEEDED
        "partial_success" -> IngestionJobStatus.PARTIAL_SUCCESS
        "failed" -> IngestionJobStatus.FAILED
        "cancelled" -> IngestionJobStatus.CANCELLED
        else -> IngestionJobStatus.QUEUED
    }
