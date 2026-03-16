package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaDocumentRepository : JpaRepository<DocumentEntity, String>
