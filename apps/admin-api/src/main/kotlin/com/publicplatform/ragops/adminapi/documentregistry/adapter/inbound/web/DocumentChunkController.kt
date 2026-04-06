package com.publicplatform.ragops.adminapi.documentregistry.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.documentregistry.application.port.`in`.SaveDocumentChunkUseCase
import com.publicplatform.ragops.documentregistry.domain.SaveDocumentChunkCommand
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 문서 청크 저장 HTTP 인바운드 어댑터.
 *
 * Python ingestion-worker가 청크·임베딩 결과를 전송하는 엔드포인트.
 * metadata 필드에 KG 추출 결과(엔티티, 토픽, 요약)를 JSON 문자열로 수신한다.
 */
@RestController
@RequestMapping("/admin")
class DocumentChunkController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val saveDocumentChunkUseCase: SaveDocumentChunkUseCase,
) {

    @PostMapping("/document-chunks")
    @ResponseStatus(HttpStatus.CREATED)
    fun saveChunk(
        @RequestBody request: SaveDocumentChunkRequest,
        servletRequest: HttpServletRequest,
    ): SaveDocumentChunkResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val command = SaveDocumentChunkCommand(
            documentId = request.documentId,
            documentVersionId = request.documentVersionId,
            chunkKey = request.chunkKey,
            chunkText = request.chunkText,
            chunkOrder = request.chunkOrder,
            tokenCount = request.tokenCount,
            embeddingVector = request.embeddingVector,
            metadata = request.metadata?.let { serializeMetadata(it) },
        )
        val saved = saveDocumentChunkUseCase.save(command)
        return SaveDocumentChunkResponse(
            id = saved.id,
            documentId = saved.documentId,
            chunkKey = saved.chunkKey,
            chunkOrder = saved.chunkOrder,
            createdAt = saved.createdAt,
        )
    }

    private fun serializeMetadata(metadata: Map<String, Any>): String {
        // Map을 JSON 문자열로 직렬화 (Jackson ObjectMapper 대신 간단한 구현)
        return com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata)
    }
}

data class SaveDocumentChunkRequest(
    val documentId: String,
    val documentVersionId: String? = null,
    val chunkKey: String,
    val chunkText: String,
    val chunkOrder: Int,
    val tokenCount: Int? = null,
    val embeddingVector: String? = null,
    val metadata: Map<String, Any>? = null,
)

data class SaveDocumentChunkResponse(
    val id: String,
    val documentId: String,
    val chunkKey: String,
    val chunkOrder: Int,
    val createdAt: Instant,
)
