package com.publicplatform.ragops.documentregistry

import java.time.Instant

enum class IngestionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

enum class IndexStatus {
    NOT_INDEXED,
    INDEXING,
    INDEXED,
    INDEX_FAILED,
}

data class DocumentSummary(
    val id: String,
    val organizationId: String,
    val documentType: String,
    val title: String,
    val sourceUri: String,
    val versionLabel: String?,
    val publishedAt: Instant?,
    val ingestionStatus: IngestionStatus,
    val indexStatus: IndexStatus,
    val visibilityScope: String,
    val lastIngestedAt: Instant?,
    val lastIndexedAt: Instant?,
    val createdAt: Instant,
)

data class DocumentVersionSummary(
    val id: String,
    val documentId: String,
    val versionLabel: String,
    val contentHash: String?,
    val sourceEtag: String?,
    val sourceLastModifiedAt: Instant?,
    val changeDetected: Boolean,
    val snapshotUri: String?,
    val parsedTextUri: String?,
    val createdAt: Instant,
)

data class DocumentScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)

interface DocumentReader {
    fun listDocuments(scope: DocumentScope): List<DocumentSummary>
}

interface DocumentVersionReader {
    fun listVersions(documentId: String): List<DocumentVersionSummary>
}

data class DocumentChunkSummary(
    val id: String,
    val documentId: String,
    val documentVersionId: String?,
    val chunkKey: String,
    val chunkText: String,
    val chunkOrder: Int,
    val tokenCount: Int?,
    val embeddingVector: String?,
    val createdAt: java.time.Instant,
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

interface DocumentWriter {
    fun saveChunk(command: SaveDocumentChunkCommand): DocumentChunkSummary
    fun deleteChunksByDocumentId(documentId: String)
}
