package com.publicplatform.ragops.documentregistry.application.service

import com.publicplatform.ragops.documentregistry.application.port.`in`.ListDocumentVersionsUseCase
import com.publicplatform.ragops.documentregistry.application.port.out.LoadDocumentVersionPort
import com.publicplatform.ragops.documentregistry.domain.DocumentVersionSummary

/**
 * 문서 버전 목록 조회 유스케이스 구현체.
 *
 * LoadDocumentVersionPort에 위임하여 특정 문서의 버전 이력을 반환한다.
 */
open class ListDocumentVersionsService(
    private val documentVersionReader: LoadDocumentVersionPort,
) : ListDocumentVersionsUseCase {

    override fun execute(documentId: String): List<DocumentVersionSummary> =
        documentVersionReader.listVersions(documentId)
}
