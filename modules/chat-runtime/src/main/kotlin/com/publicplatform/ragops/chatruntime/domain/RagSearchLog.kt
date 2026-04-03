/**
 * RAG 파이프라인 검색 이력 및 검색 결과 도메인 모델.
 *
 * 질문별로 어떤 쿼리가 실행됐고 어떤 청크가 검색됐는지를 기록하여
 * 검색 품질 분석과 KPI(zeroResultRate, avgLatencyMs) 산출에 활용한다.
 */
package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class CreateRagSearchLogCommand(
    val questionId: String,
    val queryText: String,
    val queryRewriteText: String?,
    val topK: Int?,
    val latencyMs: Int?,
    val llmMs: Int?,
    val postprocessMs: Int?,
    val retrievalEngine: String?,
    val retrievalStatus: String,
    val cacheHit: Boolean = false,
)

data class CreateRagRetrievedDocumentCommand(
    val ragSearchLogId: String,
    val documentId: String?,
    val chunkId: String?,
    val rank: Int,
    val score: Double?,
    val usedInCitation: Boolean,
)

data class QuestionContextSummary(
    val queryText: String,
    val queryRewriteText: String?,
    val latencyMs: Int?,
    val llmMs: Int?,
    val postprocessMs: Int?,
    val retrievalStatus: String,
    val retrievedChunks: List<RetrievedChunkSummary>,
)

data class RetrievedChunkSummary(
    val rank: Int,
    val score: Double?,
    val usedInCitation: Boolean,
    val chunkId: String?,
    val chunkText: String?,
)

data class RagSearchLogStats(
    val total: Int,
    val avgLatencyMs: Double?,
    val p50LatencyMs: Int?,
    val p95LatencyMs: Int?,
    val zeroResultRate: Double,
    val avgTopK: Double?,
    val retrievalStatusDistribution: Map<String, Int>,
)

data class RagSearchLogSummary(
    val id: String,
    val questionId: String,
    val queryText: String,
    val queryRewriteText: String?,
    val zeroResult: Boolean,
    val topK: Int?,
    val latencyMs: Int?,
    val llmMs: Int?,
    val postprocessMs: Int?,
    val retrievalEngine: String?,
    val retrievalStatus: String,
    val createdAt: Instant,
    val cacheHit: Boolean = false,
)
