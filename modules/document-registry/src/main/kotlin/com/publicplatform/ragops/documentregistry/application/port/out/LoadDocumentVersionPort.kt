package com.publicplatform.ragops.documentregistry.application.port.out

import com.publicplatform.ragops.documentregistry.domain.DocumentVersionSummary

interface LoadDocumentVersionPort {
    fun listVersions(documentId: String): List<DocumentVersionSummary>
}
