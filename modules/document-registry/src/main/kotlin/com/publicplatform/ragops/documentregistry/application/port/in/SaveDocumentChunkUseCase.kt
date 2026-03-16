package com.publicplatform.ragops.documentregistry.application.port.`in`

import com.publicplatform.ragops.documentregistry.domain.DocumentChunkSummary
import com.publicplatform.ragops.documentregistry.domain.SaveDocumentChunkCommand

/**
 * 문서 청크 저장 인바운드 포트.
 *
 * Python 인제스션 워커가 청크·임베딩 결과를 저장할 때 호출된다.
 */
interface SaveDocumentChunkUseCase {
    fun save(command: SaveDocumentChunkCommand): DocumentChunkSummary
    fun deleteByDocument(documentId: String)
}
