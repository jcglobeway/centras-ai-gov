package com.publicplatform.ragops.documentregistry.application.port.`in`

import com.publicplatform.ragops.documentregistry.domain.DocumentSummary
import com.publicplatform.ragops.documentregistry.domain.RegisterDocumentCommand

interface RegisterDocumentUseCase {
    fun execute(command: RegisterDocumentCommand): DocumentSummary
}
