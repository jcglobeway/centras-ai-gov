package com.publicplatform.ragops.adminapi.documentregistry

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.documentregistry.*
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin")
class DocumentController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val documentReader: DocumentReader,
    private val documentVersionReader: DocumentVersionReader,
) {

    @GetMapping("/documents")
    fun listDocuments(servletRequest: HttpServletRequest): DocumentListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()
        val documents = documentReader.listDocuments(scope)
        return DocumentListResponse(
            items = documents.map { it.toResponse() },
            total = documents.size,
        )
    }

    @GetMapping("/documents/{id}/versions")
    fun listDocumentVersions(
        @PathVariable id: String,
        servletRequest: HttpServletRequest,
    ): DocumentVersionListResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val versions = documentVersionReader.listVersions(id)
        return DocumentVersionListResponse(
            items = versions.map { it.toResponse() },
            total = versions.size,
        )
    }
}

data class DocumentListResponse(
    val items: List<DocumentResponse>,
    val total: Int,
)

data class DocumentResponse(
    val id: String,
    val organizationId: String,
    val documentType: String,
    val title: String,
    val sourceUri: String,
    val versionLabel: String?,
    val publishedAt: Instant?,
    val ingestionStatus: String,
    val indexStatus: String,
    val visibilityScope: String,
    val lastIngestedAt: Instant?,
    val lastIndexedAt: Instant?,
)

data class DocumentVersionListResponse(
    val items: List<DocumentVersionResponse>,
    val total: Int,
)

data class DocumentVersionResponse(
    val id: String,
    val documentId: String,
    val versionLabel: String,
    val contentHash: String?,
    val changeDetected: Boolean,
    val createdAt: Instant,
)

private fun DocumentSummary.toResponse(): DocumentResponse =
    DocumentResponse(
        id = id,
        organizationId = organizationId,
        documentType = documentType,
        title = title,
        sourceUri = sourceUri,
        versionLabel = versionLabel,
        publishedAt = publishedAt,
        ingestionStatus = ingestionStatus.toApiValue(),
        indexStatus = indexStatus.toApiValue(),
        visibilityScope = visibilityScope,
        lastIngestedAt = lastIngestedAt,
        lastIndexedAt = lastIndexedAt,
    )

private fun DocumentVersionSummary.toResponse(): DocumentVersionResponse =
    DocumentVersionResponse(
        id = id,
        documentId = documentId,
        versionLabel = versionLabel,
        contentHash = contentHash,
        changeDetected = changeDetected,
        createdAt = createdAt,
    )

private fun AdminSessionSnapshot.toScope(): DocumentScope =
    DocumentScope(
        organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
        globalAccess = roleAssignments.any { it.organizationId == null },
    )

private fun IngestionStatus.toApiValue(): String =
    when (this) {
        IngestionStatus.PENDING -> "pending"
        IngestionStatus.IN_PROGRESS -> "in_progress"
        IngestionStatus.COMPLETED -> "completed"
        IngestionStatus.FAILED -> "failed"
    }

private fun IndexStatus.toApiValue(): String =
    when (this) {
        IndexStatus.NOT_INDEXED -> "not_indexed"
        IndexStatus.INDEXING -> "indexing"
        IndexStatus.INDEXED -> "indexed"
        IndexStatus.INDEX_FAILED -> "index_failed"
    }
