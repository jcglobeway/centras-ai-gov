package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import com.publicplatform.ragops.documentregistry.domain.DocumentVersionSummary
import com.publicplatform.ragops.documentregistry.application.port.out.LoadDocumentVersionPort

open class LoadDocumentVersionPortAdapter(
    private val jpaRepository: JpaDocumentVersionRepository,
) : LoadDocumentVersionPort {

    override fun listVersions(documentId: String): List<DocumentVersionSummary> =
        jpaRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).map { it.toSummary() }
}
