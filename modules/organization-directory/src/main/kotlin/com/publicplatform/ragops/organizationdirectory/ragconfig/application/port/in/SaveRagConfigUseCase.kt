package com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`

import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import java.math.BigDecimal

data class SaveRagConfigCommand(
    val organizationId: String,
    val systemPrompt: String,
    val tone: String,
    val topK: Int,
    val similarityThreshold: BigDecimal,
    val rerankerEnabled: Boolean,
    val llmModel: String,
    val llmTemperature: BigDecimal,
    val llmMaxTokens: Int,
    val changeNote: String?,
    val changedBy: String?,
)

data class RollbackRagConfigCommand(
    val organizationId: String,
    val targetVersion: Int,
    val changedBy: String?,
)

interface SaveRagConfigUseCase {
    fun saveRagConfig(command: SaveRagConfigCommand): RagConfig
    fun rollback(command: RollbackRagConfigCommand): RagConfig
}
