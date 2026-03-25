package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.chatruntime.application.port.`in`.SaveRagSearchLogUseCase
import com.publicplatform.ragops.chatruntime.domain.CreateRagRetrievedDocumentCommand
import com.publicplatform.ragops.chatruntime.domain.CreateRagSearchLogCommand
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
) {

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
                retrievalEngine = request.retrievalEngine,
                retrievalStatus = request.retrievalStatus,
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
    val retrievalEngine: String?,
    val retrievalStatus: String,
    val retrievedChunks: List<RetrievedChunkRequest> = emptyList(),
)

data class RetrievedChunkRequest(
    val chunkId: String?,
    val rank: Int?,
    val score: Double?,
    val usedInCitation: Boolean?,
)

data class RagSearchLogCreatedResponse(val logId: String, val saved: Boolean)
