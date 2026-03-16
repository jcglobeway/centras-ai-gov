package com.publicplatform.ragops.chatruntime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "rag_search_logs")
class RagSearchLogEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "question_id", nullable = false)
    val questionId: String,

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    val queryText: String,

    @Column(name = "query_rewrite_text", columnDefinition = "TEXT")
    val queryRewriteText: String?,

    @Column(name = "zero_result", nullable = false)
    val zeroResult: Boolean = false,

    @Column(name = "top_k")
    val topK: Int?,

    @Column(name = "latency_ms")
    val latencyMs: Int?,

    @Column(name = "retrieval_engine", length = 50)
    val retrievalEngine: String?,

    @Column(name = "retrieval_status", nullable = false, length = 50)
    val retrievalStatus: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

fun RagSearchLogEntity.toSummary(): RagSearchLogSummary =
    RagSearchLogSummary(
        id = id,
        questionId = questionId,
        queryText = queryText,
        queryRewriteText = queryRewriteText,
        zeroResult = zeroResult,
        topK = topK,
        latencyMs = latencyMs,
        retrievalEngine = retrievalEngine,
        retrievalStatus = retrievalStatus,
        createdAt = createdAt,
    )
