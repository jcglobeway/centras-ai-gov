package com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "org_rag_configs")
open class RagConfigEntity(
    @Id
    @Column(name = "id", nullable = false)
    open val id: String,

    @Column(name = "organization_id", nullable = false)
    open val organizationId: String,

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    open val systemPrompt: String,

    @Column(name = "tone", nullable = false)
    open val tone: String,

    @Column(name = "top_k", nullable = false)
    open val topK: Int,

    @Column(name = "similarity_threshold", nullable = false, precision = 4, scale = 3)
    open val similarityThreshold: BigDecimal,

    @Column(name = "reranker_enabled", nullable = false)
    open val rerankerEnabled: Boolean,

    @Column(name = "llm_model", nullable = false)
    open val llmModel: String,

    @Column(name = "llm_temperature", nullable = false, precision = 3, scale = 2)
    open val llmTemperature: BigDecimal,

    @Column(name = "llm_max_tokens", nullable = false)
    open val llmMaxTokens: Int,

    @Column(name = "version", nullable = false)
    open val version: Int,

    @Column(name = "created_at", nullable = false)
    open val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    open val updatedAt: Instant = Instant.now(),
)

fun RagConfigEntity.toModel(): RagConfig =
    RagConfig(
        id = id,
        organizationId = organizationId,
        systemPrompt = systemPrompt,
        tone = tone,
        topK = topK,
        similarityThreshold = similarityThreshold,
        rerankerEnabled = rerankerEnabled,
        llmModel = llmModel,
        llmTemperature = llmTemperature,
        llmMaxTokens = llmMaxTokens,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RagConfig.toEntity(): RagConfigEntity =
    RagConfigEntity(
        id = id,
        organizationId = organizationId,
        systemPrompt = systemPrompt,
        tone = tone,
        topK = topK,
        similarityThreshold = similarityThreshold,
        rerankerEnabled = rerankerEnabled,
        llmModel = llmModel,
        llmTemperature = llmTemperature,
        llmMaxTokens = llmMaxTokens,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
