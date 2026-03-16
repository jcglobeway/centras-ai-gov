package com.publicplatform.ragops.ingestionops.adapter.outbound.persistence

import com.publicplatform.ragops.ingestionops.domain.CrawlSourceStatus
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStage
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStateMachine
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.InvalidIngestionJobTransitionException
import com.publicplatform.ragops.ingestionops.domain.RequestIngestionJobCommand
import com.publicplatform.ragops.ingestionops.domain.TransitionIngestionJobCommand
import com.publicplatform.ragops.ingestionops.application.port.out.PersistIngestionJobPort
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class PersistIngestionJobPortAdapter(
    private val jpaRepository: JpaIngestionJobRepository,
    private val crawlSourceRepository: JpaCrawlSourceRepository,
) : PersistIngestionJobPort {

    @Transactional
    override fun requestJob(command: RequestIngestionJobCommand): IngestionJobSummary {
        val source = crawlSourceRepository.findById(command.crawlSourceId).orElse(null)
            ?: throw InvalidIngestionJobTransitionException("존재하지 않는 crawl source 입니다: ${command.crawlSourceId}")

        val id = "ing_job_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = IngestionJobEntity(
            id = id, organizationId = source.organizationId, serviceId = source.serviceId,
            crawlSourceId = source.id, documentId = null, documentVersionId = null,
            jobType = command.jobType.name.lowercase(), jobStatus = IngestionJobStatus.QUEUED.name.lowercase(),
            jobStage = IngestionJobStage.FETCH.name.lowercase(), triggerType = command.triggerType,
            runnerType = "python_worker", attemptCount = 1, errorCode = null,
            requestedAt = command.requestedAt, requestedBy = command.requestedBy,
            startedAt = null, finishedAt = null,
            createdAt = command.requestedAt, updatedAt = command.requestedAt,
        )

        val saved = jpaRepository.save(entity)

        val updatedSource = CrawlSourceEntity(
            id = source.id, organizationId = source.organizationId, serviceId = source.serviceId,
            name = source.name, sourceType = source.sourceType, sourceUri = source.sourceUri,
            collectionMode = source.collectionMode, renderMode = source.renderMode,
            scheduleExpr = source.scheduleExpr, isActive = source.isActive, status = source.status,
            lastCrawledAt = source.lastCrawledAt, lastSucceededAt = source.lastSucceededAt,
            lastJobId = id, createdAt = source.createdAt, updatedAt = Instant.now(),
        )
        crawlSourceRepository.save(updatedSource)

        return saved.toSummary()
    }

    @Transactional
    override fun transitionJob(command: TransitionIngestionJobCommand): IngestionJobSummary {
        val currentEntity = jpaRepository.findById(command.jobId).orElse(null)
            ?: throw InvalidIngestionJobTransitionException("존재하지 않는 ingestion job 입니다: ${command.jobId}")

        val currentJob = currentEntity.toSummary()

        val transitionedJob = IngestionJobStateMachine.transition(
            current = currentJob, nextStatus = command.nextStatus, nextStage = command.nextStage,
            changedAt = command.changedAt, errorCode = command.errorCode,
        )

        val updatedEntity = IngestionJobEntity(
            id = currentEntity.id, organizationId = currentEntity.organizationId,
            serviceId = currentEntity.serviceId, crawlSourceId = currentEntity.crawlSourceId,
            documentId = currentEntity.documentId, documentVersionId = currentEntity.documentVersionId,
            jobType = currentEntity.jobType, jobStatus = transitionedJob.status.name.lowercase(),
            jobStage = transitionedJob.stage.name.lowercase(), triggerType = currentEntity.triggerType,
            runnerType = currentEntity.runnerType, attemptCount = transitionedJob.attemptCount,
            errorCode = transitionedJob.errorCode, requestedAt = currentEntity.requestedAt,
            requestedBy = currentEntity.requestedBy,
            startedAt = transitionedJob.startedAt ?: currentEntity.startedAt,
            finishedAt = transitionedJob.finishedAt ?: currentEntity.finishedAt,
            createdAt = currentEntity.createdAt, updatedAt = Instant.now(),
        )

        val saved = jpaRepository.save(updatedEntity)
        updateSourceAfterTransition(saved.toSummary())

        return saved.toSummary()
    }

    private fun updateSourceAfterTransition(job: IngestionJobSummary) {
        val source = crawlSourceRepository.findById(job.crawlSourceId).orElse(null) ?: return

        val updatedStatus = when (job.status) {
            IngestionJobStatus.FAILED -> CrawlSourceStatus.ERROR.name.lowercase()
            IngestionJobStatus.CANCELLED -> CrawlSourceStatus.PAUSED.name.lowercase()
            IngestionJobStatus.SUCCEEDED, IngestionJobStatus.PARTIAL_SUCCESS -> CrawlSourceStatus.ACTIVE.name.lowercase()
            IngestionJobStatus.QUEUED, IngestionJobStatus.RUNNING -> source.status
        }

        val updatedSource = CrawlSourceEntity(
            id = source.id, organizationId = source.organizationId, serviceId = source.serviceId,
            name = source.name, sourceType = source.sourceType, sourceUri = source.sourceUri,
            collectionMode = source.collectionMode, renderMode = source.renderMode,
            scheduleExpr = source.scheduleExpr, isActive = source.isActive, status = updatedStatus,
            lastCrawledAt = source.lastCrawledAt,
            lastSucceededAt = when (job.status) {
                IngestionJobStatus.SUCCEEDED, IngestionJobStatus.PARTIAL_SUCCESS -> job.finishedAt
                else -> source.lastSucceededAt
            },
            lastJobId = job.id, createdAt = source.createdAt, updatedAt = Instant.now(),
        )
        crawlSourceRepository.save(updatedSource)
    }
}
