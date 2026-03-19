package com.publicplatform.ragops.documentregistry.application.port.out

import com.publicplatform.ragops.documentregistry.domain.DocumentScope
import com.publicplatform.ragops.documentregistry.domain.DocumentSummary

interface LoadDocumentPort {
    fun listDocuments(scope: DocumentScope): List<DocumentSummary>
}
