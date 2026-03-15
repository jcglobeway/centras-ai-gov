package com.publicplatform.ragops.documentregistry

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaDocumentRepository : JpaRepository<DocumentEntity, String>
