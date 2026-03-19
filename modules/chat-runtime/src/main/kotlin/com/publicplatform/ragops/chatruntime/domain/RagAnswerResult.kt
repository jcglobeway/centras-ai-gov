/**
 * RAG 오케스트레이터가 반환하는 답변 생성 결과.
 *
 * RagOrchestrationPort.generateAnswer()의 반환 타입이며,
 * null이면 답변 생성을 건너뛰고 FALLBACK 처리된다.
 */
package com.publicplatform.ragops.chatruntime.domain

data class RagAnswerResult(
    val answerText: String,
    val answerStatus: String,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)
