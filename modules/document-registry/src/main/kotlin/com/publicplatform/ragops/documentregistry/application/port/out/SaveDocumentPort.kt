package com.publicplatform.ragops.documentregistry.application.port.out

import com.publicplatform.ragops.documentregistry.domain.DocumentChunkSummary
import com.publicplatform.ragops.documentregistry.domain.SaveDocumentChunkCommand

interface SaveDocumentPort {
    fun saveChunk(command: SaveDocumentChunkCommand): DocumentChunkSummary
    fun deleteChunksByDocumentId(documentId: String)
}
