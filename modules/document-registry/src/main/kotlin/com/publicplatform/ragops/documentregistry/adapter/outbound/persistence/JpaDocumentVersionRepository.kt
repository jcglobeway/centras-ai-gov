package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaDocumentVersionRepository : JpaRepository<DocumentVersionEntity, String> {
    fun findByDocumentIdOrderByCreatedAtDesc(documentId: String): List<DocumentVersionEntity>
}
