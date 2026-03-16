package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.chatruntime.application.port.`in`.ManageFeedbackUseCase
import com.publicplatform.ragops.chatruntime.domain.CreateFeedbackCommand
import com.publicplatform.ragops.chatruntime.domain.FeedbackScope
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 시민 피드백 HTTP 인바운드 어댑터.
 *
 * 피드백 생성 및 목록 조회를 ManageFeedbackUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class FeedbackController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val manageFeedbackUseCase: ManageFeedbackUseCase,
) {

    @PostMapping("/feedbacks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFeedback(
        @Valid @RequestBody request: CreateFeedbackRequest,
        servletRequest: HttpServletRequest,
    ): FeedbackCreateResponse {
        adminRequestSessionResolver.resolve(servletRequest)

        val created = manageFeedbackUseCase.create(
            CreateFeedbackCommand(
                organizationId = request.organizationId,
                serviceId = request.serviceId,
                questionId = request.questionId,
                sessionId = request.sessionId,
                rating = request.rating,
                comment = request.comment,
                channel = request.channel,
            ),
        )

        return FeedbackCreateResponse(feedbackId = created.id, rating = created.rating)
    }

    @GetMapping("/feedbacks")
    fun listFeedbacks(servletRequest: HttpServletRequest): FeedbackListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val items = manageFeedbackUseCase.list(session.toScope()).map { it.toResponse() }
        return FeedbackListResponse(items = items, total = items.size)
    }
}

data class CreateFeedbackRequest(
    @field:NotBlank val organizationId: String,
    @field:NotBlank val serviceId: String,
    val questionId: String?,
    val sessionId: String?,
    @field:Min(1) @field:Max(5) val rating: Int,
    val comment: String?,
    val channel: String?,
)

data class FeedbackCreateResponse(val feedbackId: String, val rating: Int)

data class FeedbackListResponse(val items: List<FeedbackResponse>, val total: Int)

data class FeedbackResponse(
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

private fun FeedbackSummary.toResponse() = FeedbackResponse(
    id = id, organizationId = organizationId, serviceId = serviceId,
    questionId = questionId, sessionId = sessionId, rating = rating,
    comment = comment, channel = channel, submittedAt = submittedAt,
)

private fun AdminSessionSnapshot.toScope() = FeedbackScope(
    organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
    globalAccess = roleAssignments.any { it.organizationId == null },
)
