/**
 * 질문에 대한 RAG 답변 결과 요약 뷰.
 *
 * AnswerStatus가 FALLBACK이거나 NO_ANSWER인 경우 QA 미해결 큐에 노출된다.
 * CreateAnswerCommand는 RAG 오케스트레이터가 답변을 기록할 때 사용하는 입력 커맨드이다.
 */
package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class AnswerSummary(
    val id: String,
    val questionId: String,
    val answerText: String,
    val answerStatus: AnswerStatus,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
    val createdAt: Instant,
)

data class CreateAnswerCommand(
    val questionId: String,
    val answerText: String,
    val answerStatus: AnswerStatus,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)
