package com.publicplatform.ragops.documentregistry

import java.util.UUID

open class DocumentWriterAdapter(
    private val jpaChunkRepository: JpaDocumentChunkRepository,
) : DocumentWriter {

    override fun saveChunk(command: SaveDocumentChunkCommand): DocumentChunkSummary {
        val id = "chunk_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = DocumentChunkEntity(
            id = id,
            documentId = command.documentId,
            documentVersionId = command.documentVersionId,
            chunkKey = command.chunkKey,
            chunkText = command.chunkText,
            chunkOrder = command.chunkOrder,
            tokenCount = command.tokenCount,
            embeddingVector = command.embeddingVector,
        )
        return jpaChunkRepository.save(entity).toSummary()
    }

    override fun deleteChunksByDocumentId(documentId: String) {
        jpaChunkRepository.deleteByDocumentId(documentId)
    }
}
