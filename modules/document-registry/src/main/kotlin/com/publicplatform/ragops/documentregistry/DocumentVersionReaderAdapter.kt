package com.publicplatform.ragops.documentregistry

open class DocumentVersionReaderAdapter(
    private val jpaRepository: JpaDocumentVersionRepository,
) : DocumentVersionReader {

    override fun listVersions(documentId: String): List<DocumentVersionSummary> {
        return jpaRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
            .map { it.toSummary() }
    }
}
