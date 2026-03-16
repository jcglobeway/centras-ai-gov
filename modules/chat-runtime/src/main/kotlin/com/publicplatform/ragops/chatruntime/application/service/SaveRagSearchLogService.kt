package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.SaveRagSearchLogUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.SaveRagSearchLogPort
import com.publicplatform.ragops.chatruntime.domain.CreateRagRetrievedDocumentCommand
import com.publicplatform.ragops.chatruntime.domain.CreateRagSearchLogCommand
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogSummary

/**
 * RAG 검색 로그 저장 유스케이스 구현체.
 *
 * RAG 오케스트레이터 콜백을 SaveRagSearchLogPort에 위임한다.
 */
open class SaveRagSearchLogService(
    private val ragSearchLogWriter: SaveRagSearchLogPort,
) : SaveRagSearchLogUseCase {

    override fun saveLog(command: CreateRagSearchLogCommand): RagSearchLogSummary =
        ragSearchLogWriter.saveSearchLog(command)

    override fun saveDocument(command: CreateRagRetrievedDocumentCommand) =
        ragSearchLogWriter.saveRetrievedDocument(command)
}
