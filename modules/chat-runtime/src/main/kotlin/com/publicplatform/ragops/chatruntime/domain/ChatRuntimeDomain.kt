/**
 * chat-runtime 바운디드 컨텍스트의 도메인 모델.
 *
 * 시민 채팅 세션, 질문, 답변, RAG 검색 로그, 피드백에 관한 순수 비즈니스 개념을 정의한다.
 * JPA, Spring 등 프레임워크 의존성을 포함하지 않는다.
 */
package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

enum class AnswerStatus { ANSWERED, FALLBACK, NO_ANSWER, ERROR }

data class ChatSessionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val channel: String,
    val userKeyHash: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
)

data class QuestionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val createdAt: Instant,
)

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

data class CreateQuestionCommand(
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
)

data class CreateAnswerCommand(
    val questionId: String,
    val answerText: String,
    val answerStatus: AnswerStatus,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)

data class ChatScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)

data class CreateRagSearchLogCommand(
    val questionId: String,
    val queryText: String,
    val queryRewriteText: String?,
    val topK: Int?,
    val latencyMs: Int?,
    val retrievalEngine: String?,
    val retrievalStatus: String,
)

data class CreateRagRetrievedDocumentCommand(
    val ragSearchLogId: String,
    val documentId: String?,
    val chunkId: String?,
    val rank: Int,
    val score: Double?,
    val usedInCitation: Boolean,
)

data class RagSearchLogSummary(
    val id: String,
    val questionId: String,
    val queryText: String,
    val queryRewriteText: String?,
    val zeroResult: Boolean,
    val topK: Int?,
    val latencyMs: Int?,
    val retrievalEngine: String?,
    val retrievalStatus: String,
    val createdAt: Instant,
)

data class FeedbackSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val questionId: String?,
    val sessionId: String?,
    val rating: Int,
    val comment: String?,
    val channel: String?,
    val submittedAt: Instant,
)

data class CreateFeedbackCommand(
    val organizationId: String,
    val serviceId: String,
    val questionId: String?,
    val sessionId: String?,
    val rating: Int,
    val comment: String?,
    val channel: String?,
)

data class FeedbackScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)

data class RagAnswerResult(
    val answerText: String,
    val answerStatus: String,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)
