package com.publicplatform.ragops.documentregistry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "documents")
class DocumentEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "organization_id", nullable = false)
    val organizationId: String,

    @Column(name = "document_type", nullable = false)
    val documentType: String,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "source_uri", nullable = false, columnDefinition = "TEXT")
    val sourceUri: String,

    @Column(name = "version_label")
    val versionLabel: String?,

    @Column(name = "published_at")
    val publishedAt: Instant?,

    @Column(name = "ingestion_status", nullable = false)
    val ingestionStatus: String,

    @Column(name = "index_status", nullable = false)
    val indexStatus: String,

    @Column(name = "visibility_scope", nullable = false)
    val visibilityScope: String,

    @Column(name = "last_ingested_at")
    val lastIngestedAt: Instant?,

    @Column(name = "last_indexed_at")
    val lastIndexedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

fun DocumentEntity.toSummary(): DocumentSummary =
    DocumentSummary(
        id = id,
        organizationId = organizationId,
        documentType = documentType,
        title = title,
        sourceUri = sourceUri,
        versionLabel = versionLabel,
        publishedAt = publishedAt,
        ingestionStatus = ingestionStatus.toIngestionStatus(),
        indexStatus = indexStatus.toIndexStatus(),
        visibilityScope = visibilityScope,
        lastIngestedAt = lastIngestedAt,
        lastIndexedAt = lastIndexedAt,
        createdAt = createdAt,
    )

private fun String.toIngestionStatus(): IngestionStatus =
    when (this) {
        "pending" -> IngestionStatus.PENDING
        "in_progress" -> IngestionStatus.IN_PROGRESS
        "completed" -> IngestionStatus.COMPLETED
        "failed" -> IngestionStatus.FAILED
        else -> IngestionStatus.PENDING
    }

private fun String.toIndexStatus(): IndexStatus =
    when (this) {
        "not_indexed" -> IndexStatus.NOT_INDEXED
        "indexing" -> IndexStatus.INDEXING
        "indexed" -> IndexStatus.INDEXED
        "index_failed" -> IndexStatus.INDEX_FAILED
        else -> IndexStatus.NOT_INDEXED
    }
