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
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import java.sql.Timestamp

open class SaveDocumentPortAdapter(
    private val jpaChunkRepository: JpaDocumentChunkRepository,
    private val jdbcTemplate: JdbcTemplate,
) : SaveDocumentPort {

    override fun saveChunk(command: SaveDocumentChunkCommand): DocumentChunkSummary {
        val id = "chunk_${UUID.randomUUID().toString().substring(0, 8)}"
        val createdAt = Timestamp.from(java.time.Instant.now())

        jdbcTemplate.update(
            """
            INSERT INTO document_chunks (
                id,
                document_id,
                document_version_id,
                chunk_key,
                chunk_text,
                chunk_order,
                token_count,
                embedding_vector,
                metadata,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), ?, ?)
            """.trimIndent(),
            id,
            command.documentId,
            command.documentVersionId,
            command.chunkKey,
            command.chunkText,
            command.chunkOrder,
            command.tokenCount,
            command.embeddingVector,
            command.metadata,
            createdAt,
        )

        return DocumentChunkSummary(
            id = id,
            documentId = command.documentId,
            documentVersionId = command.documentVersionId,
            chunkKey = command.chunkKey,
            chunkText = command.chunkText,
            chunkOrder = command.chunkOrder,
            tokenCount = command.tokenCount,
            embeddingVector = command.embeddingVector,
            metadata = command.metadata,
            createdAt = createdAt.toInstant(),
        )
    }

    override fun deleteChunksByDocumentId(documentId: String) {
        jpaChunkRepository.deleteByDocumentId(documentId)
    }
}
