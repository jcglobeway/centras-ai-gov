/**
 * 문서 청크 및 임베딩 도메인 모델.
 *
 * embeddingVector는 PostgreSQL에서 vector(1024) 타입으로 저장되며,
 * H2 테스트 환경에서는 TEXT로 처리한다 (V018 Kotlin 마이그레이션 참조).
 */
package com.publicplatform.ragops.documentregistry.domain

import java.time.Instant

data class DocumentChunkSummary(
    val id: String,
    val documentId: String,
    val documentVersionId: String?,
    val chunkKey: String,
    val chunkText: String,
    val chunkOrder: Int,
    val tokenCount: Int?,
    val embeddingVector: String?,
    val createdAt: Instant,
)

data class SaveDocumentChunkCommand(
    val documentId: String,
    val documentVersionId: String?,
    val chunkKey: String,
    val chunkText: String,
    val chunkOrder: Int,
    val tokenCount: Int?,
    val embeddingVector: String?,
)
