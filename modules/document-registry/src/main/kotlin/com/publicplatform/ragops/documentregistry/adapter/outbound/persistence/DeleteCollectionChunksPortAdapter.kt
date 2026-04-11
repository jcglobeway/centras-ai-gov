package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import com.publicplatform.ragops.documentregistry.application.port.`in`.DeleteCollectionResult
import com.publicplatform.ragops.documentregistry.application.port.out.DeleteCollectionChunksPort
import org.springframework.jdbc.core.JdbcTemplate

open class DeleteCollectionChunksPortAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : DeleteCollectionChunksPort {

    override fun deleteChunksByCollection(serviceId: String, collectionName: String): DeleteCollectionResult {
        val documentIds: List<String> = jdbcTemplate.queryForList(
            """
            SELECT d.id FROM documents d
            WHERE d.crawl_source_id IN (
                SELECT cs.id FROM crawl_sources cs
                WHERE cs.service_id = ? AND cs.collection_name = ?
            )
            """.trimIndent(),
            String::class.java,
            serviceId,
            collectionName,
        )

        if (documentIds.isEmpty()) {
            return DeleteCollectionResult(deletedChunks = 0, resetDocuments = 0)
        }

        val inClause = documentIds.joinToString(",") { "?" }

        val deletedChunks = jdbcTemplate.update(
            "DELETE FROM document_chunks WHERE document_id IN ($inClause)",
            *documentIds.toTypedArray(),
        )

        val resetDocuments = jdbcTemplate.update(
            "UPDATE documents SET ingestion_status = 'pending', index_status = 'not_indexed', updated_at = NOW() WHERE id IN ($inClause)",
            *documentIds.toTypedArray(),
        )

        return DeleteCollectionResult(deletedChunks = deletedChunks, resetDocuments = resetDocuments)
    }
}
