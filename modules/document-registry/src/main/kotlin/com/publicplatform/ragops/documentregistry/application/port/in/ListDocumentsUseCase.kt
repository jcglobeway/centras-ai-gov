package com.publicplatform.ragops.documentregistry.application.port.`in`

import com.publicplatform.ragops.documentregistry.domain.DocumentScope
import com.publicplatform.ragops.documentregistry.domain.DocumentSummary

/**
 * 문서 목록 조회 인바운드 포트.
 *
 * 기관 범위에 따라 필터링된 문서 목록을 반환한다.
 */
interface ListDocumentsUseCase {
    fun execute(scope: DocumentScope): List<DocumentSummary>
}
