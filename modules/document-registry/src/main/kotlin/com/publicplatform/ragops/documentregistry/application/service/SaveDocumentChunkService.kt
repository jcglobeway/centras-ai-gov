package com.publicplatform.ragops.documentregistry.application.service

import com.publicplatform.ragops.documentregistry.application.port.`in`.SaveDocumentChunkUseCase
import com.publicplatform.ragops.documentregistry.application.port.out.SaveDocumentPort
import com.publicplatform.ragops.documentregistry.domain.DocumentChunkSummary
import com.publicplatform.ragops.documentregistry.domain.SaveDocumentChunkCommand

/**
 * 문서 청크 저장 유스케이스 구현체.
 *
 * SaveDocumentPort에 위임하여 청크 저장 및 삭제를 처리한다.
 */
open class SaveDocumentChunkService(
    private val documentWriter: SaveDocumentPort,
) : SaveDocumentChunkUseCase {

    override fun save(command: SaveDocumentChunkCommand): DocumentChunkSummary =
        documentWriter.saveChunk(command)

    override fun deleteByDocument(documentId: String) =
        documentWriter.deleteChunksByDocumentId(documentId)
}
