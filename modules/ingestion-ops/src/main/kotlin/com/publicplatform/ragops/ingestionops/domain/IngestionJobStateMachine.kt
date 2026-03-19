package com.publicplatform.ragops.ingestionops.domain

import java.time.Instant

class InvalidIngestionJobTransitionException(
    message: String,
) : IllegalArgumentException(message)

object IngestionJobStateMachine {
    fun transition(
        current: IngestionJobSummary,
        nextStatus: IngestionJobStatus,
        nextStage: IngestionJobStage,
        changedAt: Instant,
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
        current: IngestionJobSummary,
        nextStatus: IngestionJobStatus,
        changedAt: Instant,
    ): Instant? =
        if (nextStatus == IngestionJobStatus.RUNNING) current.startedAt ?: changedAt else current.startedAt

    private fun resolveFinishedAt(nextStatus: IngestionJobStatus, changedAt: Instant): Instant? =
        when (nextStatus) {
            IngestionJobStatus.SUCCEEDED, IngestionJobStatus.PARTIAL_SUCCESS,
            IngestionJobStatus.FAILED, IngestionJobStatus.CANCELLED -> changedAt
            IngestionJobStatus.QUEUED, IngestionJobStatus.RUNNING -> null
        }
}
