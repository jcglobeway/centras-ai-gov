package com.publicplatform.ragops.documentregistry.application.service

import com.publicplatform.ragops.documentregistry.application.port.`in`.RegisterDocumentUseCase
import com.publicplatform.ragops.documentregistry.application.port.out.SaveDocumentRecordPort
import com.publicplatform.ragops.documentregistry.domain.DocumentSummary
import com.publicplatform.ragops.documentregistry.domain.RegisterDocumentCommand

class RegisterDocumentService(
    private val saveDocumentRecordPort: SaveDocumentRecordPort,
) : RegisterDocumentUseCase {

    override fun execute(command: RegisterDocumentCommand): DocumentSummary =
        saveDocumentRecordPort.saveDocument(command)
}
