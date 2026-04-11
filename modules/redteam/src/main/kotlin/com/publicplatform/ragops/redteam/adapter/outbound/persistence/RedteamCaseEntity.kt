package com.publicplatform.ragops.redteam.adapter.outbound.persistence

import com.publicplatform.ragops.redteam.domain.RedteamCategory
import com.publicplatform.ragops.redteam.domain.RedteamCaseSummary
import com.publicplatform.ragops.redteam.domain.RedteamExpectedBehavior
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "redteam_cases")
class RedteamCaseEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "category", nullable = false) val category: String,
    @Column(name = "title", nullable = false) val title: String,
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT") val queryText: String,
    @Column(name = "expected_behavior", nullable = false) val expectedBehavior: String,
    @Column(name = "is_active", nullable = false) val isActive: Boolean,
    @Column(name = "created_by", nullable = false) val createdBy: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
)

fun RedteamCaseEntity.toSummary(): RedteamCaseSummary =
    RedteamCaseSummary(
        id = id,
        category = category.toRedteamCategory(),
        title = title,
        queryText = queryText,
        expectedBehavior = expectedBehavior.toRedteamExpectedBehavior(),
        isActive = isActive,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RedteamCaseSummary.toEntity(): RedteamCaseEntity =
    RedteamCaseEntity(
        id = id,
        category = category.name.lowercase(),
        title = title,
        queryText = queryText,
        expectedBehavior = expectedBehavior.name.lowercase(),
        isActive = isActive,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun String.toRedteamCategory(): RedteamCategory = when (this) {
    "pii_induction" -> RedteamCategory.PII_INDUCTION
    "out_of_domain" -> RedteamCategory.OUT_OF_DOMAIN
    "prompt_injection" -> RedteamCategory.PROMPT_INJECTION
    "harmful_content" -> RedteamCategory.HARMFUL_CONTENT
    else -> RedteamCategory.OUT_OF_DOMAIN
}

private fun String.toRedteamExpectedBehavior(): RedteamExpectedBehavior = when (this) {
    "defend" -> RedteamExpectedBehavior.DEFEND
    "detect" -> RedteamExpectedBehavior.DETECT
    else -> RedteamExpectedBehavior.DEFEND
}
