package com.publicplatform.ragops.redteam.domain

import java.time.Instant

enum class RedteamCategory {
    PII_INDUCTION,
    OUT_OF_DOMAIN,
    PROMPT_INJECTION,
    HARMFUL_CONTENT,
}

enum class RedteamExpectedBehavior {
    DEFEND,
    DETECT,
}

data class RedteamCase(
    val id: String,
    val category: RedteamCategory,
    val title: String,
    val queryText: String,
    val expectedBehavior: RedteamExpectedBehavior,
    val isActive: Boolean,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class RedteamCaseSummary(
    val id: String,
    val category: RedteamCategory,
    val title: String,
    val queryText: String,
    val expectedBehavior: RedteamExpectedBehavior,
    val isActive: Boolean,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateRedteamCaseCommand(
    val category: RedteamCategory,
    val title: String,
    val queryText: String,
    val expectedBehavior: RedteamExpectedBehavior,
    val createdBy: String,
)

data class UpdateRedteamCaseCommand(
    val title: String? = null,
    val queryText: String? = null,
    val expectedBehavior: RedteamExpectedBehavior? = null,
    val isActive: Boolean? = null,
)
