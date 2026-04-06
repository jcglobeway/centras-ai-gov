package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.LoadRagSearchLogPort
import com.publicplatform.ragops.chatruntime.domain.QuestionContextSummary
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogSummary
import com.publicplatform.ragops.chatruntime.domain.RetrievedChunkSummary

open class LoadRagSearchLogPortAdapter(
    private val jpaRepository: JpaRagSearchLogRepository,
) : LoadRagSearchLogPort {

    override fun listLogsByOrganization(orgId: String?, fromDate: String?, toDate: String?): List<RagSearchLogSummary> =
        jpaRepository.findByOrgAndDateRange(orgId, fromDate, toDate).map { it.toSummary() }

    override fun getQuestionContext(questionId: String): QuestionContextSummary? {
        val rows = jpaRepository.findContextByQuestionId(questionId)
        if (rows.isEmpty()) return null
        val first = rows.first()
        return QuestionContextSummary(
            queryText = first.getQueryText(),
            queryRewriteText = first.getQueryRewriteText(),
            latencyMs = first.getLatencyMs(),
            llmMs = first.getLlmMs(),
            postprocessMs = first.getPostprocessMs(),
            retrievalStatus = first.getRetrievalStatus(),
            retrievedChunks = rows.map { row ->
                RetrievedChunkSummary(
                    rank = row.getRank(),
                    score = row.getScore()?.toDouble(),
                    usedInCitation = row.getUsedInCitation(),
                    chunkId = row.getChunkId(),
                    chunkText = row.getChunkText(),
                )
            },
        )
    }
}
