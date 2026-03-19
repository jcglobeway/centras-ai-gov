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
    val retrievalEngine: String?,
    val retrievalStatus: String,
)

data class CreateRagRetrievedDocumentCommand(
    val ragSearchLogId: String,
    val documentId: String?,
    val chunkId: String?,
    val rank: Int,
    val score: Double?,
    val usedInCitation: Boolean,
)

data class RagSearchLogSummary(
    val id: String,
    val questionId: String,
    val queryText: String,
    val queryRewriteText: String?,
    val zeroResult: Boolean,
    val topK: Int?,
    val latencyMs: Int?,
    val retrievalEngine: String?,
    val retrievalStatus: String,
    val createdAt: Instant,
)
