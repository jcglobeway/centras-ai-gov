package com.publicplatform.ragops.chatruntime

import java.math.BigDecimal
import java.util.UUID

open class RagSearchLogWriterAdapter(
    private val jpaSearchLogRepository: JpaRagSearchLogRepository,
    private val jpaRetrievedDocumentRepository: JpaRagRetrievedDocumentRepository,
) : RagSearchLogWriter {

    override fun saveSearchLog(command: CreateRagSearchLogCommand): RagSearchLogSummary {
        val id = "rsl_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = RagSearchLogEntity(
            id = id,
            questionId = command.questionId,
            queryText = command.queryText,
            queryRewriteText = command.queryRewriteText,
            zeroResult = command.topK == null || command.topK == 0,
            topK = command.topK,
            latencyMs = command.latencyMs,
            retrievalEngine = command.retrievalEngine,
            retrievalStatus = command.retrievalStatus,
        )
        return jpaSearchLogRepository.save(entity).toSummary()
    }

    override fun saveRetrievedDocument(command: CreateRagRetrievedDocumentCommand) {
        val id = "rrd_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = RagRetrievedDocumentEntity(
            id = id,
            ragSearchLogId = command.ragSearchLogId,
            documentId = command.documentId,
            chunkId = command.chunkId,
            rank = command.rank,
            score = command.score?.let { BigDecimal.valueOf(it) },
            usedInCitation = command.usedInCitation,
        )
        jpaRetrievedDocumentRepository.save(entity)
    }
}
