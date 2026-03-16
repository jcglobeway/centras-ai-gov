package com.publicplatform.ragops.documentregistry.application.service

import com.publicplatform.ragops.documentregistry.application.port.`in`.ListDocumentsUseCase
import com.publicplatform.ragops.documentregistry.application.port.out.LoadDocumentPort
import com.publicplatform.ragops.documentregistry.domain.DocumentScope
import com.publicplatform.ragops.documentregistry.domain.DocumentSummary

/**
 * 문서 목록 조회 유스케이스 구현체.
 *
 * LoadDocumentPort에 위임하여 기관 범위 내 문서 목록을 반환한다.
 */
open class ListDocumentsService(
    private val documentReader: LoadDocumentPort,
) : ListDocumentsUseCase {

    override fun execute(scope: DocumentScope): List<DocumentSummary> =
        documentReader.listDocuments(scope)
}
