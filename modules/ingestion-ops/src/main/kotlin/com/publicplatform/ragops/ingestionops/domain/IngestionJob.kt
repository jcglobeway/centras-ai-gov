/**
 * 인제스션 잡 도메인 모델 — 문서 수집·처리 작업 단위.
 *
 * 잡은 QUEUED → RUNNING → SUCCEEDED|FAILED|CANCELLED 흐름으로 진행되며,
 * 각 단계(stage)는 FETCH → EXTRACT → ... → COMPLETE 순으로 진행된다.
 * 상태 전이 유효성은 IngestionJobStateMachine에서 검증한다.
 */
package com.publicplatform.ragops.ingestionops.domain

import java.time.Instant

enum class IngestionJobStep { CRAWL, PARSE, CHUNK, EMBED, INDEX }
enum class IngestionJobStatus { QUEUED, RUNNING, SUCCEEDED, PARTIAL_SUCCESS, FAILED, CANCELLED }
enum class IngestionJobType { CRAWL, PARSE, CHUNK, EMBED, INDEX, REINDEX }
enum class IngestionJobStage { FETCH, EXTRACT, NORMALIZE, CHUNK, EMBED, INDEX, COMPLETE }

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
