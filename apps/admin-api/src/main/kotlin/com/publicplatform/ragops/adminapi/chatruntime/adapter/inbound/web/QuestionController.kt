package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.chatruntime.application.port.`in`.CreateAnswerUseCase
import com.publicplatform.ragops.chatruntime.application.port.`in`.CreateQuestionUseCase
import com.publicplatform.ragops.chatruntime.application.port.`in`.GetQuestionContextUseCase
import com.publicplatform.ragops.chatruntime.application.port.`in`.ListFaqCandidatesUseCase
import com.publicplatform.ragops.chatruntime.application.port.`in`.ListQuestionsUseCase
import com.publicplatform.ragops.chatruntime.domain.AnswerStatus
import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.FaqCandidate
import com.publicplatform.ragops.chatruntime.domain.CreateAnswerCommand
import com.publicplatform.ragops.chatruntime.domain.CreateQuestionCommand
import com.publicplatform.ragops.chatruntime.domain.QuestionContextSummary
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary
import com.publicplatform.ragops.chatruntime.domain.UnresolvedQuestionSummary
import java.math.BigDecimal
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 질문·답변 HTTP 인바운드 어댑터.
 *
 * 세션을 복원하고 UseCase를 호출한 뒤 HTTP 응답으로 변환한다.
 * 비즈니스 로직은 포함하지 않으며 CreateQuestionUseCase, ListQuestionsUseCase, CreateAnswerUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class QuestionController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val createQuestionUseCase: CreateQuestionUseCase,
    private val listQuestionsUseCase: ListQuestionsUseCase,
    private val createAnswerUseCase: CreateAnswerUseCase,
    private val listFaqCandidatesUseCase: ListFaqCandidatesUseCase,
    private val getQuestionContextUseCase: GetQuestionContextUseCase,
) {

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createQuestion(
        @Valid @RequestBody request: CreateQuestionRequest,
        servletRequest: HttpServletRequest,
    ): QuestionCreateResponse {
        adminRequestSessionResolver.resolve(servletRequest)

        val created = createQuestionUseCase.execute(
            CreateQuestionCommand(
                organizationId = request.organizationId,
                serviceId = request.serviceId,
                chatSessionId = request.chatSessionId,
                questionText = request.questionText,
                questionIntentLabel = request.questionIntentLabel,
                channel = request.channel,
                questionCategory = request.questionCategory,
            ),
        )

        return QuestionCreateResponse(questionId = created.id, created = true)
    }

    @GetMapping("/questions")
    fun listQuestions(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) from: String?,
        @RequestParam("to_date", required = false) to: String?,
        servletRequest: HttpServletRequest,
    ): QuestionListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val questions = listQuestionsUseCase.listAllWithAnswers(session.toScope(organizationId), from, to)
        return QuestionListResponse(items = questions.map { it.toResponse() }, total = questions.size)
    }

    @GetMapping("/questions/unresolved")
    fun listUnresolvedQuestions(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) from: String?,
        @RequestParam("to_date", required = false) to: String?,
        servletRequest: HttpServletRequest,
    ): UnresolvedQuestionListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val questions = listQuestionsUseCase.listUnresolved(session.toScope(organizationId), from, to)
        return UnresolvedQuestionListResponse(items = questions.map { it.toResponse() }, total = questions.size)
    }

    @GetMapping("/faq-candidates")
    fun listFaqCandidates(
        @RequestParam("organization_id") organizationId: String,
        @RequestParam("threshold", defaultValue = "0.85") threshold: Double,
        servletRequest: HttpServletRequest,
    ): FaqCandidateListResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val candidates = listFaqCandidatesUseCase.list(organizationId, threshold)
        return FaqCandidateListResponse(items = candidates.map { it.toResponse() }, total = candidates.size)
    }

    @GetMapping("/questions/{questionId}/context")
    fun getQuestionContext(
        @PathVariable questionId: String,
        servletRequest: HttpServletRequest,
    ): QuestionContextResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val ctx = getQuestionContextUseCase.getContext(questionId)
            ?: return QuestionContextResponse(queryText = null, queryRewriteText = null, latencyMs = null, llmMs = null, postprocessMs = null, retrievalStatus = null, retrievedChunks = emptyList())
        return ctx.toResponse()
    }

    @PostMapping("/answers")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnswer(
        @Valid @RequestBody request: CreateAnswerRequest,
        servletRequest: HttpServletRequest,
    ): AnswerCreateResponse {
        adminRequestSessionResolver.resolve(servletRequest)

        val created = createAnswerUseCase.execute(
            CreateAnswerCommand(
                questionId = request.questionId,
                answerText = request.answerText,
                answerStatus = request.answerStatus.toAnswerStatus(),
                responseTimeMs = request.responseTimeMs,
                citationCount = request.citationCount,
                fallbackReasonCode = request.fallbackReasonCode,
            ),
        )

        return AnswerCreateResponse(
            answerId = created.id,
            questionId = created.questionId,
            answerStatus = created.answerStatus.toApiValue(),
        )
    }
}

data class CreateQuestionRequest(
    @field:NotBlank val organizationId: String,
    @field:NotBlank val serviceId: String,
    @field:NotBlank val chatSessionId: String,
    @field:NotBlank val questionText: String,
    val questionIntentLabel: String?,
    @field:NotBlank val channel: String,
    val questionCategory: String? = null,
)

data class QuestionCreateResponse(val questionId: String, val created: Boolean)

data class QuestionListResponse(val items: List<QuestionResponse>, val total: Int)

data class QuestionResponse(
    val questionId: String,
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val questionCategory: String?,
    val failureReasonCode: String?,
    val isEscalated: Boolean,
    val answerConfidence: BigDecimal?,
    val createdAt: Instant,
    val answerText: String?,
    val answerStatus: String?,
    val responseTimeMs: Int?,
    val faithfulness: Double?,
    val answerRelevancy: Double?,
    val contextPrecision: Double?,
    val contextRecall: Double?,
)

data class UnresolvedQuestionListResponse(val items: List<UnresolvedQuestionResponse>, val total: Int)

data class UnresolvedQuestionResponse(
    val questionId: String,
    val organizationId: String,
    val questionText: String,
    val failureReasonCode: String?,
    val questionCategory: String?,
    val isEscalated: Boolean,
    val answerStatus: String?,
    val latestReviewStatus: String?,
    val latestReviewId: String?,
    val assigneeId: String?,
    val createdAt: Instant,
)

data class CreateAnswerRequest(
    @field:NotBlank val questionId: String,
    @field:NotBlank val answerText: String,
    @field:NotBlank val answerStatus: String,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)

data class AnswerCreateResponse(val answerId: String, val questionId: String, val answerStatus: String)

data class FaqCandidateListResponse(val items: List<FaqCandidateResponse>, val total: Int)

data class FaqCandidateResponse(
    val questionId: String,
    val questionText: String,
    val questionCategory: String?,
    val similarId: String,
    val similarText: String,
    val similarity: Double,
)

private fun FaqCandidate.toResponse() = FaqCandidateResponse(
    questionId = questionId,
    questionText = questionText,
    questionCategory = questionCategory,
    similarId = similarId,
    similarText = similarText,
    similarity = similarity,
)

private fun QuestionSummary.toResponse() = QuestionResponse(
    questionId = id, organizationId = organizationId, serviceId = serviceId,
    chatSessionId = chatSessionId, questionText = questionText,
    questionIntentLabel = questionIntentLabel, channel = channel,
    questionCategory = questionCategory, failureReasonCode = failureReasonCode?.code,
    isEscalated = isEscalated, answerConfidence = answerConfidence, createdAt = createdAt,
    answerText = answerText, answerStatus = answerStatus, responseTimeMs = responseTimeMs,
    faithfulness = faithfulness, answerRelevancy = answerRelevancy,
    contextPrecision = contextPrecision, contextRecall = contextRecall,
)

private fun UnresolvedQuestionSummary.toResponse() = UnresolvedQuestionResponse(
    questionId = questionId, organizationId = organizationId, questionText = questionText,
    failureReasonCode = failureReasonCode?.code, questionCategory = questionCategory,
    isEscalated = isEscalated, answerStatus = answerStatus,
    latestReviewStatus = latestReviewStatus, latestReviewId = latestReviewId,
    assigneeId = latestReviewAssigneeId, createdAt = createdAt,
)

data class QuestionContextResponse(
    val queryText: String?,
    val queryRewriteText: String?,
    val latencyMs: Int?,
    val llmMs: Int?,
    val postprocessMs: Int?,
    val retrievalStatus: String?,
    val retrievedChunks: List<RetrievedChunkResponse>,
)

data class RetrievedChunkResponse(
    val rank: Int,
    val score: Double?,
    val usedInCitation: Boolean,
    val chunkId: String?,
    val chunkText: String?,
)

private fun QuestionContextSummary.toResponse() = QuestionContextResponse(
    queryText = queryText,
    queryRewriteText = queryRewriteText,
    latencyMs = latencyMs,
    llmMs = llmMs,
    postprocessMs = postprocessMs,
    retrievalStatus = retrievalStatus,
    retrievedChunks = retrievedChunks.map { chunk ->
        RetrievedChunkResponse(
            rank = chunk.rank,
            score = chunk.score,
            usedInCitation = chunk.usedInCitation,
            chunkId = chunk.chunkId,
            chunkText = chunk.chunkText,
        )
    },
)

private fun AdminSessionSnapshot.toScope(filterOrgId: String? = null): ChatScope {
    val globalAccess = roleAssignments.any { it.organizationId == null }
    val sessionOrgIds = roleAssignments.mapNotNull { it.organizationId }.toSet()
    return if (filterOrgId != null) {
        val allowed = globalAccess || filterOrgId in sessionOrgIds
        ChatScope(organizationIds = if (allowed) setOf(filterOrgId) else sessionOrgIds, globalAccess = false)
    } else {
        ChatScope(organizationIds = sessionOrgIds, globalAccess = globalAccess)
    }
}

private fun String.toAnswerStatus(): AnswerStatus =
    try { AnswerStatus.fromStringStrict(this) }
    catch (e: IllegalArgumentException) {
        throw org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST, e.message,
        )
    }

private fun AnswerStatus.toApiValue(): String = value
