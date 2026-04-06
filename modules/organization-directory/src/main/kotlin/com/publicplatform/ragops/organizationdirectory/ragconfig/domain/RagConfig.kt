package com.publicplatform.ragops.organizationdirectory.ragconfig.domain

import java.math.BigDecimal
import java.time.Instant

data class RagConfig(
    val id: String,
    val organizationId: String,
    val systemPrompt: String,
    val tone: String,
    val topK: Int,
    val similarityThreshold: BigDecimal,
    val rerankerEnabled: Boolean,
    val llmModel: String,
    val llmTemperature: BigDecimal,
    val llmMaxTokens: Int,
    val version: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class RagConfigVersion(
    val id: String,
    val organizationId: String,
    val version: Int,
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
    val createdAt: Instant,
)
