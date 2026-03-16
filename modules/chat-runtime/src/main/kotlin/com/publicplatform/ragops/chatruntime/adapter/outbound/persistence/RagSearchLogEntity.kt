/**
 * RagSearchLog DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.RagSearchLogSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "rag_search_logs")
class RagSearchLogEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "question_id", nullable = false) val questionId: String,
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT") val queryText: String,
    @Column(name = "query_rewrite_text", columnDefinition = "TEXT") val queryRewriteText: String?,
    @Column(name = "zero_result", nullable = false) val zeroResult: Boolean = false,
    @Column(name = "top_k") val topK: Int?,
    @Column(name = "latency_ms") val latencyMs: Int?,
    @Column(name = "retrieval_engine", length = 50) val retrievalEngine: String?,
    @Column(name = "retrieval_status", nullable = false, length = 50) val retrievalStatus: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

fun RagSearchLogEntity.toSummary(): RagSearchLogSummary =
    RagSearchLogSummary(
        id = id, questionId = questionId, queryText = queryText, queryRewriteText = queryRewriteText,
        zeroResult = zeroResult, topK = topK, latencyMs = latencyMs,
        retrievalEngine = retrievalEngine, retrievalStatus = retrievalStatus, createdAt = createdAt,
    )
