package com.publicplatform.ragops.adminapi.documentregistry.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.documentregistry.application.port.`in`.ListDocumentVersionsUseCase
import com.publicplatform.ragops.documentregistry.application.port.`in`.ListDocumentsUseCase
import com.publicplatform.ragops.documentregistry.domain.DocumentScope
import com.publicplatform.ragops.documentregistry.domain.DocumentSummary
import com.publicplatform.ragops.documentregistry.domain.DocumentVersionSummary
import com.publicplatform.ragops.documentregistry.domain.IndexStatus
import com.publicplatform.ragops.documentregistry.domain.IngestionStatus
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 문서 레지스트리 HTTP 인바운드 어댑터.
 *
 * 문서 목록 및 버전 목록 조회를 ListDocumentsUseCase, ListDocumentVersionsUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class DocumentController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val listDocumentsUseCase: ListDocumentsUseCase,
    private val listDocumentVersionsUseCase: ListDocumentVersionsUseCase,
) {

    @GetMapping("/documents")
    fun listDocuments(servletRequest: HttpServletRequest): DocumentListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val documents = listDocumentsUseCase.execute(session.toScope())
        return DocumentListResponse(items = documents.map { it.toResponse() }, total = documents.size)
    }

    @GetMapping("/documents/{id}/versions")
    fun listDocumentVersions(
        @PathVariable id: String,
        servletRequest: HttpServletRequest,
    ): DocumentVersionListResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val versions = listDocumentVersionsUseCase.execute(id)
        return DocumentVersionListResponse(items = versions.map { it.toResponse() }, total = versions.size)
    }
}

data class DocumentListResponse(val items: List<DocumentResponse>, val total: Int)

data class DocumentResponse(
    val id: String, val organizationId: String, val documentType: String, val title: String,
    val sourceUri: String, val versionLabel: String?, val publishedAt: Instant?,
    val ingestionStatus: String, val indexStatus: String, val visibilityScope: String,
    val lastIngestedAt: Instant?, val lastIndexedAt: Instant?,
)

data class DocumentVersionListResponse(val items: List<DocumentVersionResponse>, val total: Int)

data class DocumentVersionResponse(
    val id: String, val documentId: String, val versionLabel: String,
    val contentHash: String?, val changeDetected: Boolean, val createdAt: Instant,
)

private fun DocumentSummary.toResponse() = DocumentResponse(
    id = id, organizationId = organizationId, documentType = documentType, title = title,
    sourceUri = sourceUri, versionLabel = versionLabel, publishedAt = publishedAt,
    ingestionStatus = ingestionStatus.toApiValue(), indexStatus = indexStatus.toApiValue(),
    visibilityScope = visibilityScope, lastIngestedAt = lastIngestedAt, lastIndexedAt = lastIndexedAt,
)

private fun DocumentVersionSummary.toResponse() = DocumentVersionResponse(
    id = id, documentId = documentId, versionLabel = versionLabel,
    contentHash = contentHash, changeDetected = changeDetected, createdAt = createdAt,
)

private fun AdminSessionSnapshot.toScope() = DocumentScope(
    organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
    globalAccess = roleAssignments.any { it.organizationId == null },
)

private fun IngestionStatus.toApiValue() = when (this) {
    IngestionStatus.PENDING -> "pending"; IngestionStatus.IN_PROGRESS -> "in_progress"
    IngestionStatus.COMPLETED -> "completed"; IngestionStatus.FAILED -> "failed"
}

private fun IndexStatus.toApiValue() = when (this) {
    IndexStatus.NOT_INDEXED -> "not_indexed"; IndexStatus.INDEXING -> "indexing"
    IndexStatus.INDEXED -> "indexed"; IndexStatus.INDEX_FAILED -> "index_failed"
}
