/**
 * 문서 버전 이력 요약 뷰.
 *
 * contentHash와 sourceEtag로 콘텐츠 변경 여부를 감지하며,
 * changeDetected=true이면 재인제스션 대상임을 의미한다.
 */
package com.publicplatform.ragops.documentregistry.domain

import java.time.Instant

data class DocumentVersionSummary(
    val id: String,
    val documentId: String,
    val versionLabel: String,
    val contentHash: String?,
    val sourceEtag: String?,
    val sourceLastModifiedAt: Instant?,
    val changeDetected: Boolean,
    val snapshotUri: String?,
    val parsedTextUri: String?,
    val createdAt: Instant,
)
