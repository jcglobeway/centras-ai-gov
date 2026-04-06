package com.publicplatform.ragops.organizationdirectory.ragconfig.application.service

import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.RollbackRagConfigCommand
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.SaveRagConfigCommand
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.SaveRagConfigUseCase
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.out.LoadRagConfigPort
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.out.RecordRagConfigPort
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfigVersion
import java.time.Instant
import java.util.UUID

open class SaveRagConfigService(
    private val loadRagConfigPort: LoadRagConfigPort,
    private val recordRagConfigPort: RecordRagConfigPort,
) : SaveRagConfigUseCase {

    override fun saveRagConfig(command: SaveRagConfigCommand): RagConfig {
        val existing = loadRagConfigPort.loadByOrganizationId(command.organizationId)
        val now = Instant.now()
        val newVersion = (existing?.version ?: 0) + 1

        val config = RagConfig(
            id = existing?.id ?: "rag_cfg_${UUID.randomUUID().toString().substring(0, 8)}",
            organizationId = command.organizationId,
            systemPrompt = command.systemPrompt,
            tone = command.tone,
            topK = command.topK,
            similarityThreshold = command.similarityThreshold,
            rerankerEnabled = command.rerankerEnabled,
            llmModel = command.llmModel,
            llmTemperature = command.llmTemperature,
            llmMaxTokens = command.llmMaxTokens,
            version = newVersion,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        val saved = recordRagConfigPort.save(config)

        val versionRecord = RagConfigVersion(
            id = "rag_ver_${UUID.randomUUID().toString().substring(0, 8)}",
            organizationId = command.organizationId,
            version = newVersion,
            systemPrompt = command.systemPrompt,
            tone = command.tone,
            topK = command.topK,
            similarityThreshold = command.similarityThreshold,
            rerankerEnabled = command.rerankerEnabled,
            llmModel = command.llmModel,
            llmTemperature = command.llmTemperature,
            llmMaxTokens = command.llmMaxTokens,
            changeNote = command.changeNote,
            changedBy = command.changedBy,
            createdAt = now,
        )
        recordRagConfigPort.saveVersion(versionRecord)

        return saved
    }

    override fun rollback(command: RollbackRagConfigCommand): RagConfig {
        val targetVersion = loadRagConfigPort.loadVersion(command.organizationId, command.targetVersion)
            ?: throw IllegalArgumentException("버전 ${command.targetVersion}을 찾을 수 없습니다: ${command.organizationId}")

        return saveRagConfig(
            SaveRagConfigCommand(
                organizationId = command.organizationId,
                systemPrompt = targetVersion.systemPrompt,
                tone = targetVersion.tone,
                topK = targetVersion.topK,
                similarityThreshold = targetVersion.similarityThreshold,
                rerankerEnabled = targetVersion.rerankerEnabled,
                llmModel = targetVersion.llmModel,
                llmTemperature = targetVersion.llmTemperature,
                llmMaxTokens = targetVersion.llmMaxTokens,
                changeNote = "v${command.targetVersion} 롤백",
                changedBy = command.changedBy,
            )
        )
    }
}
