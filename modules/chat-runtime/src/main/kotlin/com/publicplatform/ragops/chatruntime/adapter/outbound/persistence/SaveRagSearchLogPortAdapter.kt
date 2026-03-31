/**
 * SaveRagSearchLogPort의 JPA 구현체.
 *
 * RAG 오케스트레이터가 콜백으로 전달한 검색 로그와 검색된 문서 목록을 저장한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.CreateRagRetrievedDocumentCommand
import com.publicplatform.ragops.chatruntime.domain.CreateRagSearchLogCommand
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogSummary
import com.publicplatform.ragops.chatruntime.application.port.out.SaveRagSearchLogPort
import java.math.BigDecimal
import java.util.UUID

open class SaveRagSearchLogPortAdapter(
    private val jpaSearchLogRepository: JpaRagSearchLogRepository,
    private val jpaRetrievedDocumentRepository: JpaRagRetrievedDocumentRepository,
) : SaveRagSearchLogPort {

    override fun saveSearchLog(command: CreateRagSearchLogCommand): RagSearchLogSummary {
        val id = "rsl_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = RagSearchLogEntity(
            id = id, questionId = command.questionId, queryText = command.queryText,
            queryRewriteText = command.queryRewriteText,
            zeroResult = command.topK == null || command.topK == 0,
            topK = command.topK, latencyMs = command.latencyMs, llmMs = command.llmMs,
            retrievalEngine = command.retrievalEngine, retrievalStatus = command.retrievalStatus,
        )
        return jpaSearchLogRepository.save(entity).toSummary()
    }

    override fun saveRetrievedDocument(command: CreateRagRetrievedDocumentCommand) {
        val id = "rrd_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = RagRetrievedDocumentEntity(
            id = id, ragSearchLogId = command.ragSearchLogId,
            documentId = command.documentId, chunkId = command.chunkId,
            rank = command.rank, score = command.score?.let { BigDecimal.valueOf(it) },
            usedInCitation = command.usedInCitation,
        )
        jpaRetrievedDocumentRepository.save(entity)
    }
}
