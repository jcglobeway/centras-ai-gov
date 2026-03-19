/**
 * RAG 검색 로그 저장 아웃바운드 포트.
 *
 * Python rag-orchestrator가 /admin/rag-search-logs 콜백을 통해 검색 이력과
 * 검색 결과 문서 목록을 Admin API로 전달하면 이 포트가 저장을 처리한다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.CreateRagRetrievedDocumentCommand
import com.publicplatform.ragops.chatruntime.domain.CreateRagSearchLogCommand
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogSummary

interface SaveRagSearchLogPort {
    fun saveSearchLog(command: CreateRagSearchLogCommand): RagSearchLogSummary
    fun saveRetrievedDocument(command: CreateRagRetrievedDocumentCommand)
}
