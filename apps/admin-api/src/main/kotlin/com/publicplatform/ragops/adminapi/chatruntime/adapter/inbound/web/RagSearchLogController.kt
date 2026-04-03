package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.chatruntime.application.port.`in`.GetRagSearchLogStatsUseCase
import com.publicplatform.ragops.chatruntime.application.port.`in`.SaveRagSearchLogUseCase
import com.publicplatform.ragops.chatruntime.domain.CreateRagRetrievedDocumentCommand
import com.publicplatform.ragops.chatruntime.domain.CreateRagSearchLogCommand
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogStats
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * RAG 검색 로그 인바운드 어댑터.
 *
 * rag-orchestrator가 pgvector 검색 결과를 콜백으로 보낼 때 호출된다.
 * rag_search_logs 및 rag_retrieved_documents 테이블에 저장한다.
 */
@RestController
@RequestMapping("/admin")
class RagSearchLogController(
    private val saveRagSearchLogUseCase: SaveRagSearchLogUseCase,
    private val getRagSearchLogStatsUseCase: GetRagSearchLogStatsUseCase,
) {

    @GetMapping("/rag-search-logs")
    fun getRagSearchLogStats(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) fromDate: String?,
        @RequestParam("to_date", required = false) toDate: String?,
        servletRequest: HttpServletRequest,
    ): RagSearchLogStats = getRagSearchLogStatsUseCase.getStats(organizationId, fromDate, toDate)

    @PostMapping("/rag-search-logs")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRagSearchLog(
        @RequestBody request: CreateRagSearchLogRequest,
        servletRequest: HttpServletRequest,
    ): RagSearchLogCreatedResponse {
        val log = saveRagSearchLogUseCase.saveLog(
            CreateRagSearchLogCommand(
                questionId = request.questionId,
                queryText = request.queryText,
                queryRewriteText = null,
                topK = request.topK,
                latencyMs = request.latencyMs,
                llmMs = request.llmMs,
                postprocessMs = request.postprocessMs,
                retrievalEngine = request.retrievalEngine,
                retrievalStatus = request.retrievalStatus,
                cacheHit = request.cacheHit,
            ),
        )

        request.retrievedChunks.forEachIndexed { idx, chunk ->
            saveRagSearchLogUseCase.saveDocument(
                CreateRagRetrievedDocumentCommand(
                    ragSearchLogId = log.id,
                    documentId = null,
                    chunkId = chunk.chunkId,
                    rank = chunk.rank ?: (idx + 1),
                    score = chunk.score,
                    usedInCitation = chunk.usedInCitation ?: true,
                ),
            )
        }

        return RagSearchLogCreatedResponse(logId = log.id, saved = true)
    }
}

data class CreateRagSearchLogRequest(
    val questionId: String,
    val queryText: String,
    val topK: Int?,
    val latencyMs: Int?,
    val llmMs: Int?,
    val postprocessMs: Int?,
    val retrievalEngine: String?,
    val retrievalStatus: String,
    val retrievedChunks: List<RetrievedChunkRequest> = emptyList(),
    val cacheHit: Boolean = false,
)

data class RetrievedChunkRequest(
    val chunkId: String?,
    val rank: Int?,
    val score: Double?,
    val usedInCitation: Boolean?,
)

data class RagSearchLogCreatedResponse(val logId: String, val saved: Boolean)
