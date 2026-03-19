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
