package com.publicplatform.ragops.documentregistry

open class DocumentReaderAdapter(
    private val jpaRepository: JpaDocumentRepository,
) : DocumentReader {

    override fun listDocuments(scope: DocumentScope): List<DocumentSummary> {
        val allDocuments = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) {
            allDocuments
        } else {
            allDocuments.filter { it.organizationId in scope.organizationIds }
        }
    }
}
