/**
 * LoadDocumentVersionPort의 JPA 구현체.
 *
 * 특정 문서의 인제스션 버전 이력을 반환한다.
 */
package com.publicplatform.ragops.documentregistry.adapter.outbound.persistence

import com.publicplatform.ragops.documentregistry.domain.DocumentVersionSummary
import com.publicplatform.ragops.documentregistry.application.port.out.LoadDocumentVersionPort

open class LoadDocumentVersionPortAdapter(
    private val jpaRepository: JpaDocumentVersionRepository,
) : LoadDocumentVersionPort {

    override fun listVersions(documentId: String): List<DocumentVersionSummary> =
        jpaRepository.findByDocumentIdOrderByCreatedAtDesc(documentId).map { it.toSummary() }
}
