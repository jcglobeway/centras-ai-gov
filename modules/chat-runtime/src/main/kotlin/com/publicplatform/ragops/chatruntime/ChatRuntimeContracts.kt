package com.publicplatform.ragops.chatruntime

import java.time.Instant

enum class AnswerStatus {
    ANSWERED,
    FALLBACK,
    NO_ANSWER,
    ERROR,
}

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

interface QuestionReader {
    fun listQuestions(scope: ChatScope): List<QuestionSummary>
    fun listUnresolvedQuestions(scope: ChatScope): List<QuestionSummary>
}

interface QuestionWriter {
    fun createQuestion(command: CreateQuestionCommand): QuestionSummary
}

interface AnswerReader {
    fun findByQuestionId(questionId: String): AnswerSummary?
}

interface AnswerWriter {
    fun createAnswer(command: CreateAnswerCommand): AnswerSummary
}

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
    val createdAt: java.time.Instant,
)

interface RagSearchLogWriter {
    fun saveSearchLog(command: CreateRagSearchLogCommand): RagSearchLogSummary
    fun saveRetrievedDocument(command: CreateRagRetrievedDocumentCommand)
}
