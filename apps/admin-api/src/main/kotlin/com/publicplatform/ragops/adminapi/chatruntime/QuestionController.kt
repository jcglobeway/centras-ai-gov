package com.publicplatform.ragops.adminapi.chatruntime

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.chatruntime.*
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/admin")
class QuestionController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val questionReader: QuestionReader,
    private val questionWriter: QuestionWriter,
    private val answerReader: AnswerReader,
    private val answerWriter: AnswerWriter,
) {

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createQuestion(
        @Valid @RequestBody request: CreateQuestionRequest,
        servletRequest: HttpServletRequest,
    ): QuestionCreateResponse {
        adminRequestSessionResolver.resolve(servletRequest)

        val createdQuestion = questionWriter.createQuestion(
            CreateQuestionCommand(
                organizationId = request.organizationId,
                serviceId = request.serviceId,
                chatSessionId = request.chatSessionId,
                questionText = request.questionText,
                questionIntentLabel = request.questionIntentLabel,
                channel = request.channel,
            ),
        )

        return QuestionCreateResponse(
            questionId = createdQuestion.id,
            created = true,
        )
    }

    @GetMapping("/questions")
    fun listQuestions(servletRequest: HttpServletRequest): QuestionListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()
        val questions = questionReader.listQuestions(scope)
        return QuestionListResponse(
            items = questions.map { it.toResponse() },
            total = questions.size,
        )
    }

    @GetMapping("/questions/unresolved")
    fun listUnresolvedQuestions(servletRequest: HttpServletRequest): QuestionListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope()
        val questions = questionReader.listUnresolvedQuestions(scope)
        return QuestionListResponse(
            items = questions.map { it.toResponse() },
            total = questions.size,
        )
    }

    @PostMapping("/answers")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAnswer(
        @Valid @RequestBody request: CreateAnswerRequest,
        servletRequest: HttpServletRequest,
    ): AnswerCreateResponse {
        adminRequestSessionResolver.resolve(servletRequest)

        val createdAnswer = answerWriter.createAnswer(
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
            answerId = createdAnswer.id,
            questionId = createdAnswer.questionId,
            answerStatus = createdAnswer.answerStatus.toApiValue(),
        )
    }
}

data class CreateQuestionRequest(
    @field:NotBlank
    val organizationId: String,
    @field:NotBlank
    val serviceId: String,
    @field:NotBlank
    val chatSessionId: String,
    @field:NotBlank
    val questionText: String,
    val questionIntentLabel: String?,
    @field:NotBlank
    val channel: String,
)

data class QuestionCreateResponse(
    val questionId: String,
    val created: Boolean,
)

data class QuestionListResponse(
    val items: List<QuestionResponse>,
    val total: Int,
)

data class QuestionResponse(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val createdAt: Instant,
)

data class CreateAnswerRequest(
    @field:NotBlank
    val questionId: String,
    @field:NotBlank
    val answerText: String,
    @field:NotBlank
    val answerStatus: String,
    val responseTimeMs: Int?,
    val citationCount: Int?,
    val fallbackReasonCode: String?,
)

data class AnswerCreateResponse(
    val answerId: String,
    val questionId: String,
    val answerStatus: String,
)

private fun QuestionSummary.toResponse(): QuestionResponse =
    QuestionResponse(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        chatSessionId = chatSessionId,
        questionText = questionText,
        questionIntentLabel = questionIntentLabel,
        channel = channel,
        createdAt = createdAt,
    )

private fun AdminSessionSnapshot.toScope(): ChatScope =
    ChatScope(
        organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
        globalAccess = roleAssignments.any { it.organizationId == null },
    )

private fun String.toAnswerStatus(): AnswerStatus =
    when (this) {
        "answered" -> AnswerStatus.ANSWERED
        "fallback" -> AnswerStatus.FALLBACK
        "no_answer" -> AnswerStatus.NO_ANSWER
        "error" -> AnswerStatus.ERROR
        else -> throw org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST,
            "Invalid answer_status: $this",
        )
    }

private fun AnswerStatus.toApiValue(): String =
    when (this) {
        AnswerStatus.ANSWERED -> "answered"
        AnswerStatus.FALLBACK -> "fallback"
        AnswerStatus.NO_ANSWER -> "no_answer"
        AnswerStatus.ERROR -> "error"
    }
