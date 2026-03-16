/**
 * Organization DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "organizations")
class OrganizationEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "org_code", nullable = false, unique = true)
    val orgCode: String,

    @Column(name = "status", nullable = false)
    val status: String,

    @Column(name = "institution_type", nullable = false)
    val institutionType: String,

    @Column(name = "owner_user_id")
    val ownerUserId: String?,

    @Column(name = "last_document_sync_at")
    val lastDocumentSyncAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

fun OrganizationEntity.toModel(): Organization =
    Organization(
        id = id, name = name, orgCode = orgCode, status = status,
        institutionType = institutionType, ownerUserId = ownerUserId,
        lastDocumentSyncAt = lastDocumentSyncAt, createdAt = createdAt,
    )

fun OrganizationEntity.toSummary(): OrganizationSummary =
    OrganizationSummary(id = id, name = name, institutionType = institutionType)

fun Organization.toEntity(): OrganizationEntity =
    OrganizationEntity(
        id = id, name = name, orgCode = orgCode, status = status,
        institutionType = institutionType, ownerUserId = ownerUserId,
        lastDocumentSyncAt = lastDocumentSyncAt, createdAt = createdAt, updatedAt = Instant.now(),
    )
