package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.chatruntime.application.port.`in`.ListChatSessionsUseCase
import com.publicplatform.ragops.chatruntime.application.port.`in`.ListQuestionsUseCase
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.ChatSessionSummary
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import java.math.BigDecimal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin")
class ChatSessionController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val listChatSessionsUseCase: ListChatSessionsUseCase,
    private val listQuestionsUseCase: ListQuestionsUseCase,
) {

    @GetMapping("/chat-sessions")
    fun listChatSessions(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) from: String?,
        @RequestParam("to_date", required = false) to: String?,
        servletRequest: HttpServletRequest,
    ): ChatSessionListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val sessions = listChatSessionsUseCase.listSessions(session.toScope(organizationId), from, to)
        return ChatSessionListResponse(items = sessions.map { it.toResponse() }, total = sessions.size)
    }

    @GetMapping("/chat-sessions/{sessionId}")
    fun getChatSession(
        @PathVariable sessionId: String,
        servletRequest: HttpServletRequest,
    ): ChatSessionResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        return listChatSessionsUseCase.getSession(sessionId)?.toResponse()
            ?: throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Session not found: $sessionId",
            )
    }

    @GetMapping("/chat-sessions/{sessionId}/questions")
    fun listSessionQuestions(
        @PathVariable sessionId: String,
        servletRequest: HttpServletRequest,
    ): SessionQuestionListResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val questions = listQuestionsUseCase.listBySession(sessionId)
        return SessionQuestionListResponse(items = questions.map { it.toResponse() }, total = questions.size)
    }
}

data class ChatSessionListResponse(val items: List<ChatSessionResponse>, val total: Int)

data class ChatSessionResponse(
    val sessionId: String,
    val organizationId: String,
    val serviceId: String,
    val channel: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val sessionEndType: String?,
    val totalQuestionCount: Int,
)

data class SessionQuestionListResponse(val items: List<SessionQuestionResponse>, val total: Int)

data class SessionQuestionResponse(
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

private fun QuestionSummary.toResponse() = SessionQuestionResponse(
    questionId = id,
    organizationId = organizationId,
    serviceId = serviceId,
    chatSessionId = chatSessionId,
    questionText = questionText,
    questionIntentLabel = questionIntentLabel,
    channel = channel,
    questionCategory = questionCategory,
    failureReasonCode = failureReasonCode?.code,
    isEscalated = isEscalated,
    answerConfidence = answerConfidence,
    createdAt = createdAt,
    answerText = answerText,
    answerStatus = answerStatus,
    responseTimeMs = responseTimeMs,
    faithfulness = faithfulness,
    answerRelevancy = answerRelevancy,
    contextPrecision = contextPrecision,
    contextRecall = contextRecall,
)

private fun ChatSessionSummary.toResponse() = ChatSessionResponse(
    sessionId = id,
    organizationId = organizationId,
    serviceId = serviceId,
    channel = channel,
    startedAt = startedAt,
    endedAt = endedAt,
    sessionEndType = sessionEndType,
    totalQuestionCount = totalQuestionCount,
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
