/**
 * Answer DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.AnswerStatus
import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "answers")
class AnswerEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "question_id", nullable = false) val questionId: String,
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT") val answerText: String,
    @Column(name = "answer_status", nullable = false) val answerStatus: String,
    @Column(name = "response_time_ms") val responseTimeMs: Int?,
    @Column(name = "citation_count") val citationCount: Int?,
    @Column(name = "fallback_reason_code") val fallbackReasonCode: String?,
    @Column(name = "model_name") val modelName: String?,
    @Column(name = "provider_name") val providerName: String?,
    @Column(name = "input_tokens") val inputTokens: Int?,
    @Column(name = "output_tokens") val outputTokens: Int?,
    @Column(name = "total_tokens") val totalTokens: Int?,
    @Column(name = "estimated_cost_usd") val estimatedCostUsd: Double?,
    @Column(name = "finish_reason") val finishReason: String?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

fun AnswerEntity.toSummary(): AnswerSummary =
    AnswerSummary(
        id = id, questionId = questionId, answerText = answerText,
        answerStatus = AnswerStatus.fromString(answerStatus), responseTimeMs = responseTimeMs,
        citationCount = citationCount, fallbackReasonCode = fallbackReasonCode,
        modelName = modelName, providerName = providerName,
        inputTokens = inputTokens, outputTokens = outputTokens, totalTokens = totalTokens,
        estimatedCostUsd = estimatedCostUsd, finishReason = finishReason,
        createdAt = createdAt,
    )
