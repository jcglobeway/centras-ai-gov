package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaDocumentChunkRepository : JpaRepository<DocumentChunkEntity, String> {
    fun findByDocumentId(documentId: String): List<DocumentChunkEntity>
    fun deleteByDocumentId(documentId: String)
}
