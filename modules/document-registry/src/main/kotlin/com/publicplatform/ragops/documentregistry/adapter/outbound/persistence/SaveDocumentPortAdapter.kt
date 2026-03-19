/**
 * SaveDocumentPort의 JPA 구현체.
 *
 * Python 인제스션 워커가 전송한 청크·임베딩 데이터를 document_chunks 테이블에 저장한다.
 * 재인제스션 시 deleteChunksByDocumentId로 기존 청크를 먼저 삭제해야 한다.
 */
package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import com.publicplatform.ragops.documentregistry.domain.DocumentChunkSummary
import com.publicplatform.ragops.documentregistry.domain.SaveDocumentChunkCommand
import com.publicplatform.ragops.documentregistry.application.port.out.SaveDocumentPort
import java.util.UUID

open class SaveDocumentPortAdapter(
    private val jpaChunkRepository: JpaDocumentChunkRepository,
) : SaveDocumentPort {

    override fun saveChunk(command: SaveDocumentChunkCommand): DocumentChunkSummary {
        val id = "chunk_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = DocumentChunkEntity(
            id = id, documentId = command.documentId, documentVersionId = command.documentVersionId,
            chunkKey = command.chunkKey, chunkText = command.chunkText,
            chunkOrder = command.chunkOrder, tokenCount = command.tokenCount,
            embeddingVector = command.embeddingVector,
        )
        return jpaChunkRepository.save(entity).toSummary()
    }

    override fun deleteChunksByDocumentId(documentId: String) {
        jpaChunkRepository.deleteByDocumentId(documentId)
    }
}
