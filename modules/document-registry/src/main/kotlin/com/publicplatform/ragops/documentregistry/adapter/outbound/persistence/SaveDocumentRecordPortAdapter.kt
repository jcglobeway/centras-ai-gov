package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import com.publicplatform.ragops.documentregistry.application.port.out.SaveDocumentRecordPort
import com.publicplatform.ragops.documentregistry.domain.DocumentSummary
import com.publicplatform.ragops.documentregistry.domain.RegisterDocumentCommand
import java.time.Instant
import java.util.UUID

open class SaveDocumentRecordPortAdapter(
    private val jpaDocumentRepository: JpaDocumentRepository,
) : SaveDocumentRecordPort {

    override fun saveDocument(command: RegisterDocumentCommand): DocumentSummary {
        val id = "doc_${UUID.randomUUID().toString().substring(0, 8)}"
        val now = Instant.now()
        val entity = DocumentEntity(
            id = id,
            organizationId = command.organizationId,
            documentType = command.documentType,
            title = command.title,
            sourceUri = command.sourceUri,
            versionLabel = null,
            publishedAt = null,
            ingestionStatus = "pending",
            indexStatus = "not_indexed",
            visibilityScope = command.visibilityScope,
            lastIngestedAt = null,
            lastIndexedAt = null,
            createdAt = now,
            updatedAt = now,
            collectionName = command.collectionName,
            crawlSourceId = command.crawlSourceId,
        )
        return jpaDocumentRepository.save(entity).toSummary()
    }
}
