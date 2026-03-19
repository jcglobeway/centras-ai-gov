/**
 * 문서 메타데이터 도메인 모델.
 *
 * ingestionStatus(수집 상태)와 indexStatus(벡터 인덱스 상태)를 독립적으로 추적하여
 * 수집은 완료됐지만 인덱싱이 미완료된 문서를 식별할 수 있다.
 * DocumentScope는 기관별 접근 제어에 사용된다.
 */
package com.publicplatform.ragops.documentregistry.domain

import java.time.Instant

enum class IngestionStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED }
enum class IndexStatus { NOT_INDEXED, INDEXING, INDEXED, INDEX_FAILED }

data class DocumentSummary(
    val id: String,
    val organizationId: String,
    val documentType: String,
    val title: String,
    val sourceUri: String,
    val versionLabel: String?,
    val publishedAt: Instant?,
    val ingestionStatus: IngestionStatus,
    val indexStatus: IndexStatus,
    val visibilityScope: String,
    val lastIngestedAt: Instant?,
    val lastIndexedAt: Instant?,
    val createdAt: Instant,
)

data class DocumentScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
