package com.publicplatform.ragops.adminapi.ingestion

import com.publicplatform.ragops.ingestionops.CrawlCollectionMode
import com.publicplatform.ragops.ingestionops.CrawlRenderMode
import com.publicplatform.ragops.ingestionops.CrawlSourceReader
import com.publicplatform.ragops.ingestionops.CrawlSourceStatus
import com.publicplatform.ragops.ingestionops.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.CrawlSourceType
import com.publicplatform.ragops.ingestionops.CrawlSourceWriter
import com.publicplatform.ragops.ingestionops.CreateCrawlSourceCommand
import com.publicplatform.ragops.ingestionops.IngestionJobReader
import com.publicplatform.ragops.ingestionops.IngestionJobStage
import com.publicplatform.ragops.ingestionops.IngestionJobStateMachine
import com.publicplatform.ragops.ingestionops.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.IngestionJobType
import com.publicplatform.ragops.ingestionops.IngestionJobWriter
import com.publicplatform.ragops.ingestionops.IngestionScope
import com.publicplatform.ragops.ingestionops.InvalidIngestionJobTransitionException
import com.publicplatform.ragops.ingestionops.RequestIngestionJobCommand
import com.publicplatform.ragops.ingestionops.TransitionIngestionJobCommand
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@Service
class DevelopmentIngestionStore :
    CrawlSourceReader,
    CrawlSourceWriter,
    IngestionJobReader,
    IngestionJobWriter {
    private val sourceSequence = AtomicInteger(300)
    private val jobSequence = AtomicInteger(900)

    private val sources =
        CopyOnWriteArrayList(
            listOf(
                CrawlSourceSummary(
                    id = "crawl_src_001",
                    organizationId = "org_seoul_120",
                    serviceId = "svc_welfare",
                    name = "Seoul Notices",
                    sourceType = CrawlSourceType.WEBSITE,
                    sourceUri = "https://seoul.example.go.kr/notices",
                    renderMode = CrawlRenderMode.BROWSER_PLAYWRIGHT,
                    collectionMode = CrawlCollectionMode.INCREMENTAL,
                    schedule = "0 */6 * * *",
                    status = CrawlSourceStatus.ACTIVE,
                    lastSucceededAt = Instant.parse("2026-03-15T01:20:00Z"),
                    lastJobId = "ing_job_101",
                ),
                CrawlSourceSummary(
                    id = "crawl_src_002",
                    organizationId = "org_busan_220",
                    serviceId = "svc_faq",
                    name = "Busan FAQ Sitemap",
                    sourceType = CrawlSourceType.SITEMAP,
                    sourceUri = "https://busan.example.go.kr/faq/sitemap.xml",
                    renderMode = CrawlRenderMode.HTTP_STATIC,
                    collectionMode = CrawlCollectionMode.INCREMENTAL,
                    schedule = "0 */12 * * *",
                    status = CrawlSourceStatus.PAUSED,
                    lastSucceededAt = Instant.parse("2026-03-14T22:10:00Z"),
                    lastJobId = "ing_job_202",
                ),
            ),
        )

    private val jobs =
        CopyOnWriteArrayList(
            listOf(
                IngestionJobSummary(
                    id = "ing_job_101",
                    organizationId = "org_seoul_120",
                    serviceId = "svc_welfare",
                    crawlSourceId = "crawl_src_001",
                    documentId = "doc_301",
                    jobType = IngestionJobType.CRAWL,
                    stage = IngestionJobStage.COMPLETE,
                    status = IngestionJobStatus.SUCCEEDED,
                    runnerType = "python_worker",
                    triggerType = "scheduled",
                    attemptCount = 1,
                    errorCode = null,
                    requestedAt = Instant.parse("2026-03-15T01:00:00Z"),
                    startedAt = Instant.parse("2026-03-15T01:01:00Z"),
                    finishedAt = Instant.parse("2026-03-15T01:20:00Z"),
                ),
                IngestionJobSummary(
                    id = "ing_job_202",
                    organizationId = "org_busan_220",
                    serviceId = "svc_faq",
                    crawlSourceId = "crawl_src_002",
                    documentId = null,
                    jobType = IngestionJobType.CRAWL,
                    stage = IngestionJobStage.COMPLETE,
                    status = IngestionJobStatus.FAILED,
                    runnerType = "python_worker",
                    triggerType = "manual",
                    attemptCount = 2,
                    errorCode = "CRAWL_TIMEOUT",
                    requestedAt = Instant.parse("2026-03-14T22:00:00Z"),
                    startedAt = Instant.parse("2026-03-14T22:01:00Z"),
                    finishedAt = Instant.parse("2026-03-14T22:03:00Z"),
                ),
            ),
        )

    override fun listSources(scope: IngestionScope): List<CrawlSourceSummary> =
        sources.filterSourcesByScope(scope)

    override fun createSource(command: CreateCrawlSourceCommand): CrawlSourceSummary {
        val createdSource =
            CrawlSourceSummary(
                id = "crawl_src_${sourceSequence.incrementAndGet()}",
                organizationId = command.organizationId,
                serviceId = command.serviceId,
                name = command.name,
                sourceType = command.sourceType,
                sourceUri = command.sourceUri,
                renderMode = command.renderMode,
                collectionMode = command.collectionMode,
                schedule = command.schedule,
                status = CrawlSourceStatus.ACTIVE,
                lastSucceededAt = null,
                lastJobId = null,
            )

        sources += createdSource
        return createdSource
    }

    override fun listJobs(scope: IngestionScope): List<IngestionJobSummary> =
        jobs.filterJobsByScope(scope)

    override fun requestJob(command: RequestIngestionJobCommand): IngestionJobSummary {
        val source =
            sources.firstOrNull { it.id == command.crawlSourceId }
                ?: throw InvalidIngestionJobTransitionException("존재하지 않는 crawl source 입니다: ${command.crawlSourceId}")

        val createdJob =
            IngestionJobSummary(
                id = "ing_job_${jobSequence.incrementAndGet()}",
                organizationId = source.organizationId,
                serviceId = source.serviceId,
                crawlSourceId = source.id,
                documentId = null,
                jobType = command.jobType,
                stage = IngestionJobStage.FETCH,
                status = IngestionJobStatus.QUEUED,
                runnerType = "python_worker",
                triggerType = command.triggerType,
                attemptCount = 1,
                errorCode = null,
                requestedAt = command.requestedAt,
                startedAt = null,
                finishedAt = null,
            )

        jobs += createdJob
        updateSourceLastJob(source.id, createdJob.id)
        return createdJob
    }

    override fun transitionJob(command: TransitionIngestionJobCommand): IngestionJobSummary {
        val currentJob =
            jobs.firstOrNull { it.id == command.jobId }
                ?: throw InvalidIngestionJobTransitionException("존재하지 않는 ingestion job 입니다: ${command.jobId}")

        // 상태 전이 규칙은 모듈 로직을 그대로 사용해 API와 저장소 구현이 다른 판단을 하지 않게 맞춘다.
        val transitionedJob =
            IngestionJobStateMachine.transition(
                current = currentJob,
                nextStatus = command.nextStatus,
                nextStage = command.nextStage,
                changedAt = command.changedAt,
                errorCode = command.errorCode,
            )

        replaceJob(currentJob, transitionedJob)
        updateSourceAfterTransition(transitionedJob)
        return transitionedJob
    }

    private fun updateSourceLastJob(
        crawlSourceId: String,
        lastJobId: String,
    ) {
        val source = sources.firstOrNull { it.id == crawlSourceId } ?: return
        replaceSource(source, source.copy(lastJobId = lastJobId))
    }

    private fun updateSourceAfterTransition(job: IngestionJobSummary) {
        val source = sources.firstOrNull { it.id == job.crawlSourceId } ?: return
        val updatedStatus =
            when (job.status) {
                IngestionJobStatus.FAILED -> CrawlSourceStatus.ERROR
                IngestionJobStatus.CANCELLED -> CrawlSourceStatus.PAUSED
                IngestionJobStatus.SUCCEEDED,
                IngestionJobStatus.PARTIAL_SUCCESS,
                -> CrawlSourceStatus.ACTIVE
                IngestionJobStatus.QUEUED,
                IngestionJobStatus.RUNNING,
                -> source.status
            }

        // source 상태는 최신 job 결과를 요약해서 보여주기 위한 운영용 projection 이다.
        replaceSource(
            current = source,
            updated =
                source.copy(
                    status = updatedStatus,
                    lastSucceededAt =
                        when (job.status) {
                            IngestionJobStatus.SUCCEEDED,
                            IngestionJobStatus.PARTIAL_SUCCESS,
                            -> job.finishedAt
                            else -> source.lastSucceededAt
                        },
                    lastJobId = job.id,
                ),
        )
    }

    private fun replaceSource(
        current: CrawlSourceSummary,
        updated: CrawlSourceSummary,
    ) {
        val index = sources.indexOf(current)
        if (index >= 0) {
            sources[index] = updated
        }
    }

    private fun replaceJob(
        current: IngestionJobSummary,
        updated: IngestionJobSummary,
    ) {
        val index = jobs.indexOf(current)
        if (index >= 0) {
            jobs[index] = updated
        }
    }
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
