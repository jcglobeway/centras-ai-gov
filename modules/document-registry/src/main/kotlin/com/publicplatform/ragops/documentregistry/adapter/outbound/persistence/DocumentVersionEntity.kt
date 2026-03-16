/**
 * DocumentVersion DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import com.publicplatform.ragops.documentregistry.domain.DocumentVersionSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "document_versions")
class DocumentVersionEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "document_id", nullable = false) val documentId: String,
    @Column(name = "version_label", nullable = false) val versionLabel: String,
    @Column(name = "content_hash") val contentHash: String?,
    @Column(name = "source_etag") val sourceEtag: String?,
    @Column(name = "source_last_modified_at") val sourceLastModifiedAt: Instant?,
    @Column(name = "change_detected", nullable = false) val changeDetected: Boolean = false,
    @Column(name = "snapshot_uri", columnDefinition = "TEXT") val snapshotUri: String?,
    @Column(name = "parsed_text_uri", columnDefinition = "TEXT") val parsedTextUri: String?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

fun DocumentVersionEntity.toSummary(): DocumentVersionSummary =
    DocumentVersionSummary(
        id = id, documentId = documentId, versionLabel = versionLabel, contentHash = contentHash,
        sourceEtag = sourceEtag, sourceLastModifiedAt = sourceLastModifiedAt,
        changeDetected = changeDetected, snapshotUri = snapshotUri, parsedTextUri = parsedTextUri,
        createdAt = createdAt,
    )
