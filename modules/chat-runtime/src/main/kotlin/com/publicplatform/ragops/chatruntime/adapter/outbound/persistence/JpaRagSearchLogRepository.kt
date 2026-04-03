/**
 * RagSearchLog 관련 Spring Data JPA 레포지토리.
 *
 * Adapter 클래스에서만 사용하며, RepositoryConfiguration을 통해 주입된다.
 * Controller나 Service가 직접 참조하면 ArchUnit Rule 5가 실패한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

interface RetrievedChunkRow {
    fun getRank(): Int
    fun getScore(): BigDecimal?
    fun getUsedInCitation(): Boolean
    fun getChunkId(): String?
    fun getChunkText(): String?
    fun getQueryText(): String
    fun getQueryRewriteText(): String?
    fun getLatencyMs(): Int?
    fun getLlmMs(): Int?
    fun getPostprocessMs(): Int?
    fun getRetrievalStatus(): String
}

@Repository
interface JpaRagSearchLogRepository : JpaRepository<RagSearchLogEntity, String> {
    fun findByQuestionId(questionId: String): List<RagSearchLogEntity>

    @Query(value = """
        SELECT rrd.rank AS rank, rrd.score AS score, rrd.used_in_citation AS usedInCitation,
               rrd.chunk_id AS chunkId, dc.chunk_text AS chunkText,
               rsl.query_text AS queryText, rsl.query_rewrite_text AS queryRewriteText,
               rsl.latency_ms AS latencyMs, rsl.llm_ms AS llmMs, rsl.postprocess_ms AS postprocessMs,
               rsl.retrieval_status AS retrievalStatus
        FROM rag_search_logs rsl
        JOIN rag_retrieved_documents rrd ON rrd.rag_search_log_id = rsl.id
        LEFT JOIN document_chunks dc ON dc.id = rrd.chunk_id
        WHERE rsl.question_id = :questionId
        ORDER BY rrd.rank
    """, nativeQuery = true)
    fun findContextByQuestionId(@Param("questionId") questionId: String): List<RetrievedChunkRow>

    @Query(value = """
        SELECT rsl.id, rsl.question_id, rsl.query_text, rsl.query_rewrite_text,
               rsl.zero_result, rsl.top_k, rsl.latency_ms, rsl.llm_ms, rsl.postprocess_ms,
               rsl.retrieval_engine, rsl.retrieval_status, rsl.cache_hit, rsl.created_at
        FROM rag_search_logs rsl
        JOIN questions q ON q.id = rsl.question_id
        WHERE (:orgId IS NULL OR q.organization_id = :orgId)
          AND (:fromDate IS NULL OR DATE(rsl.created_at) >= CAST(:fromDate AS date))
          AND (:toDate IS NULL OR DATE(rsl.created_at) <= CAST(:toDate AS date))
        ORDER BY rsl.created_at DESC
    """, nativeQuery = true)
    fun findByOrgAndDateRange(
        @Param("orgId") orgId: String?,
        @Param("fromDate") fromDate: String?,
        @Param("toDate") toDate: String?,
    ): List<RagSearchLogEntity>
}

@Repository
interface JpaRagRetrievedDocumentRepository : JpaRepository<RagRetrievedDocumentEntity, String> {
    fun findByRagSearchLogId(ragSearchLogId: String): List<RagRetrievedDocumentEntity>
}
