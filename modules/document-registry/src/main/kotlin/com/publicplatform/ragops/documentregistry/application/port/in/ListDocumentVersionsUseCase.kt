package com.publicplatform.ragops.documentregistry.application.port.`in`

import com.publicplatform.ragops.documentregistry.domain.DocumentVersionSummary

/**
 * 문서 버전 목록 조회 인바운드 포트.
 *
 * 특정 문서의 인제스션 이력(버전 목록)을 반환한다.
 */
interface ListDocumentVersionsUseCase {
    fun execute(documentId: String): List<DocumentVersionSummary>
}
