package com.publicplatform.ragops.documentregistry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "document_chunks")
class DocumentChunkEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "document_id", nullable = false)
    val documentId: String,

    @Column(name = "document_version_id")
    val documentVersionId: String?,

    @Column(name = "chunk_key", nullable = false)
    val chunkKey: String,

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    val chunkText: String,

    @Column(name = "chunk_order", nullable = false)
    val chunkOrder: Int,

    @Column(name = "token_count")
    val tokenCount: Int?,

    // H2(테스트): TEXT로 저장, PostgreSQL(운영): V018로 vector(1024) 타입으로 변환
    @Column(name = "embedding_vector", columnDefinition = "TEXT")
    val embeddingVector: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

fun DocumentChunkEntity.toSummary(): DocumentChunkSummary =
    DocumentChunkSummary(
        id = id,
        documentId = documentId,
        documentVersionId = documentVersionId,
        chunkKey = chunkKey,
        chunkText = chunkText,
        chunkOrder = chunkOrder,
        tokenCount = tokenCount,
        embeddingVector = embeddingVector,
        createdAt = createdAt,
    )
