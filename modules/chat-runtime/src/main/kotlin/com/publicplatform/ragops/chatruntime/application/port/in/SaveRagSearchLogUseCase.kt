package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.CreateRagRetrievedDocumentCommand
import com.publicplatform.ragops.chatruntime.domain.CreateRagSearchLogCommand
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogSummary

/**
 * RAG 검색 로그 저장 인바운드 포트.
 *
 * RAG 오케스트레이터가 검색 결과를 콜백으로 보낼 때 호출된다.
 * 검색 품질 모니터링 및 KPI 산출에 사용된다.
 */
interface SaveRagSearchLogUseCase {
    fun saveLog(command: CreateRagSearchLogCommand): RagSearchLogSummary
    fun saveDocument(command: CreateRagRetrievedDocumentCommand)
}
