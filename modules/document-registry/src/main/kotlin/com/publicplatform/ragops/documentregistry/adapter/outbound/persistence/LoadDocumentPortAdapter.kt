/**
 * LoadDocumentPort의 JPA 구현체.
 *
 * 기관 범위에 따라 문서 목록을 필터링하여 반환한다.
 */
package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import com.publicplatform.ragops.documentregistry.domain.DocumentScope
import com.publicplatform.ragops.documentregistry.domain.DocumentSummary
import com.publicplatform.ragops.documentregistry.application.port.out.LoadDocumentPort

open class LoadDocumentPortAdapter(
    private val jpaRepository: JpaDocumentRepository,
) : LoadDocumentPort {

    override fun listDocuments(scope: DocumentScope): List<DocumentSummary> {
        val allDocuments = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) allDocuments
        else allDocuments.filter { it.organizationId in scope.organizationIds }
    }
}
