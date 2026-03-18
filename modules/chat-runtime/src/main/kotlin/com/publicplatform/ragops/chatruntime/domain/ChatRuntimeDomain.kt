/**
 * chat-runtime 바운디드 컨텍스트의 도메인 모델.
 *
 * 시민 채팅 세션, 질문, 답변, RAG 검색 로그, 피드백에 관한 순수 비즈니스 개념을 정의한다.
 * JPA, Spring 등 프레임워크 의존성을 포함하지 않는다.
 */
package com.publicplatform.ragops.chatruntime.domain

import java.math.BigDecimal
import java.time.Instant

enum class AnswerStatus { ANSWERED, FALLBACK, NO_ANSWER, ERROR }

/**
 * RAG 파이프라인 실패 원인 표준 코드 taxonomy (A01~A10).
 *
 * Python worker 또는 QA 리뷰어가 questions.failure_reason_code 컬럼에 기록한다.
 * A01~A05는 파이프라인 결함, A06~A07은 모델 결함, A08~A10은 외부 요인이다.
 */
enum class FailureReasonCode(val code: String, val description: String) {
    A01("A01", "관련 문서 없음 — 지식 공백"),
    A02("A02", "문서 있으나 최신 아님 — 오래된 콘텐츠"),
    A03("A03", "파싱 실패 — HTML/PDF 처리 오류"),
    A04("A04", "검색 실패 — 검색 결과 0건"),
    A05("A05", "재랭킹 실패 — reranking 오류"),
    A06("A06", "생성 답변 왜곡 — hallucination"),
    A07("A07", "질문 의도 분류 실패"),
    A08("A08", "정책상 답변 제한"),
    A09("A09", "질문 표현 모호함"),
    A10("A10", "채널 UI/입력 문제");

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: String): FailureReasonCode =
            byCode[code] ?: throw InvalidFailureReasonCodeException(
                "알 수 없는 실패 원인 코드: $code. 허용 코드: ${byCode.keys.sorted().joinToString()}"
            )

        fun isValid(code: String): Boolean = byCode.containsKey(code)
    }
}

class InvalidFailureReasonCodeException(message: String) : RuntimeException(message)

data class ChatSessionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val channel: String,
    val userKeyHash: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val sessionEndType: String?,
    val totalQuestionCount: Int,
)

data class QuestionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val questionCategory: String?,
    val answerConfidence: BigDecimal?,
    val failureReasonCode: String?,
    val isEscalated: Boolean,
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
    val failureReasonCode: String? = null,
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
    val feedbackType: String?,
    val clickedLink: Boolean,
    val clickedDocument: Boolean,
    val targetActionType: String?,
    val targetActionCompleted: Boolean,
    val dwellTimeMs: Long?,
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
    val feedbackType: String? = null,
    val clickedLink: Boolean = false,
    val clickedDocument: Boolean = false,
    val targetActionType: String? = null,
    val targetActionCompleted: Boolean = false,
    val dwellTimeMs: Long? = null,
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
