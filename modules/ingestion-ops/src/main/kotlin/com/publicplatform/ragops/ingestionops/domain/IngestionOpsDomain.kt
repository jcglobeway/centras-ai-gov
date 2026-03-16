/**
 * ingestion-ops 바운디드 컨텍스트의 도메인 모델.
 *
 * 크롤 소스와 인제스션 잡의 생명주기를 관리한다.
 * IngestionJobStateMachine은 허용된 상태 전이 규칙을 캡슐화한다.
 */
package com.publicplatform.ragops.ingestionops.domain

import java.time.Instant

enum class CrawlSourceType { WEBSITE, SITEMAP, FILE_DROP }
enum class CrawlSourceStatus { ACTIVE, PAUSED, ERROR }
enum class IngestionJobStep { CRAWL, PARSE, CHUNK, EMBED, INDEX }
enum class IngestionJobStatus { QUEUED, RUNNING, SUCCEEDED, PARTIAL_SUCCESS, FAILED, CANCELLED }
enum class IngestionJobType { CRAWL, PARSE, CHUNK, EMBED, INDEX, REINDEX }
enum class IngestionJobStage { FETCH, EXTRACT, NORMALIZE, CHUNK, EMBED, INDEX, COMPLETE }
enum class CrawlRenderMode { HTTP_STATIC, BROWSER_PLAYWRIGHT, BROWSER_LIGHTPANDA }
enum class CrawlCollectionMode { FULL, INCREMENTAL }

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

class InvalidIngestionJobTransitionException(
    message: String,
) : IllegalArgumentException(message)

object IngestionJobStateMachine {
    fun transition(
        current: IngestionJobSummary,
        nextStatus: IngestionJobStatus,
        nextStage: IngestionJobStage,
        changedAt: java.time.Instant,
        errorCode: String?,
    ): IngestionJobSummary {
        requireAllowedStatus(current.status, nextStatus)
        requireAllowedStage(nextStatus, nextStage)

        return current.copy(
            status = nextStatus,
            stage = nextStage,
            errorCode = errorCode,
            startedAt = resolveStartedAt(current, nextStatus, changedAt),
            finishedAt = resolveFinishedAt(nextStatus, changedAt),
        )
    }

    private fun requireAllowedStatus(currentStatus: IngestionJobStatus, nextStatus: IngestionJobStatus) {
        val allowedNextStatuses = when (currentStatus) {
            IngestionJobStatus.QUEUED -> setOf(IngestionJobStatus.RUNNING, IngestionJobStatus.CANCELLED)
            IngestionJobStatus.RUNNING -> setOf(
                IngestionJobStatus.RUNNING, IngestionJobStatus.SUCCEEDED,
                IngestionJobStatus.PARTIAL_SUCCESS, IngestionJobStatus.FAILED, IngestionJobStatus.CANCELLED,
            )
            IngestionJobStatus.SUCCEEDED, IngestionJobStatus.PARTIAL_SUCCESS,
            IngestionJobStatus.FAILED, IngestionJobStatus.CANCELLED -> emptySet()
        }
        if (nextStatus !in allowedNextStatuses) {
            throw InvalidIngestionJobTransitionException(
                "허용되지 않은 ingestion job 상태 전이입니다: $currentStatus -> $nextStatus",
            )
        }
    }

    private fun requireAllowedStage(nextStatus: IngestionJobStatus, nextStage: IngestionJobStage) {
        val allowedStages = when (nextStatus) {
            IngestionJobStatus.QUEUED -> setOf(IngestionJobStage.FETCH)
            IngestionJobStatus.RUNNING -> setOf(
                IngestionJobStage.FETCH, IngestionJobStage.EXTRACT, IngestionJobStage.NORMALIZE,
                IngestionJobStage.CHUNK, IngestionJobStage.EMBED, IngestionJobStage.INDEX,
            )
            IngestionJobStatus.SUCCEEDED, IngestionJobStatus.PARTIAL_SUCCESS,
            IngestionJobStatus.FAILED, IngestionJobStatus.CANCELLED -> setOf(IngestionJobStage.COMPLETE)
        }
        if (nextStage !in allowedStages) {
            throw InvalidIngestionJobTransitionException(
                "허용되지 않은 ingestion job 단계입니다: status=$nextStatus, stage=$nextStage",
            )
        }
    }

    private fun resolveStartedAt(
        current: IngestionJobSummary, nextStatus: IngestionJobStatus, changedAt: java.time.Instant,
    ): java.time.Instant? =
        if (nextStatus == IngestionJobStatus.RUNNING) current.startedAt ?: changedAt else current.startedAt

    private fun resolveFinishedAt(nextStatus: IngestionJobStatus, changedAt: java.time.Instant): java.time.Instant? =
        when (nextStatus) {
            IngestionJobStatus.SUCCEEDED, IngestionJobStatus.PARTIAL_SUCCESS,
            IngestionJobStatus.FAILED, IngestionJobStatus.CANCELLED -> changedAt
            IngestionJobStatus.QUEUED, IngestionJobStatus.RUNNING -> null
        }
}
