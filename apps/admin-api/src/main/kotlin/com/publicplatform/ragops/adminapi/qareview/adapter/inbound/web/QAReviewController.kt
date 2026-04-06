package com.publicplatform.ragops.adminapi.qareview.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationException
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.identityaccess.domain.AuthorizationFailureReason
import com.publicplatform.ragops.qareview.application.port.`in`.AssignQAReviewUseCase
import com.publicplatform.ragops.qareview.application.port.`in`.CreateQAReviewUseCase
import com.publicplatform.ragops.qareview.application.port.`in`.ListQAReviewsUseCase
import com.publicplatform.ragops.qareview.domain.ActionType
import com.publicplatform.ragops.qareview.domain.CreateQAReviewCommand
import com.publicplatform.ragops.qareview.domain.InvalidQAReviewException
import com.publicplatform.ragops.qareview.domain.QAReviewStatus
import com.publicplatform.ragops.qareview.domain.QAReviewSummary
import com.publicplatform.ragops.qareview.domain.RootCauseCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

/**
 * QA 리뷰 HTTP 인바운드 어댑터.
 *
 * 리뷰 생성 및 목록 조회를 CreateQAReviewUseCase, ListQAReviewsUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class QAReviewController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val createQAReviewUseCase: CreateQAReviewUseCase,
    private val listQAReviewsUseCase: ListQAReviewsUseCase,
    private val assignQAReviewUseCase: AssignQAReviewUseCase,
) {

    @PostMapping("/qa-reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun createQAReview(
        @Valid @RequestBody request: CreateQAReviewRequest,
        servletRequest: HttpServletRequest,
    ): QAReviewCreateResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "qa.review.write")

        val created = try {
            createQAReviewUseCase.execute(
                CreateQAReviewCommand(
                    questionId = request.questionId,
                    reviewStatus = request.reviewStatus.toReviewStatus(),
                    rootCauseCode = request.rootCauseCode?.toRootCauseCode(),
                    actionType = request.actionType?.toActionType(),
                    actionTargetId = request.actionTargetId,
                    reviewComment = request.reviewComment,
                    reviewerId = session.user.id,
                    assigneeId = request.assigneeId,
                ),
            )
        } catch (exception: InvalidQAReviewException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, exception.message, exception)
        }

        return QAReviewCreateResponse(
            qaReviewId = created.id,
            questionId = created.questionId,
            reviewStatus = created.reviewStatus.toApiValue(),
        )
    }

    @GetMapping("/qa-reviews")
    fun listQAReviews(
        @RequestParam(required = false) questionId: String?,
        @RequestParam("review_status", required = false) reviewStatus: String?,
        servletRequest: HttpServletRequest,
    ): QAReviewListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "qa.review.read")

        val reviews = when {
            questionId != null -> listQAReviewsUseCase.listByQuestion(questionId)
            reviewStatus != null -> listQAReviewsUseCase.listByStatus(reviewStatus)
            else -> listQAReviewsUseCase.listAll()
        }

        return QAReviewListResponse(items = reviews.map { it.toResponse() }, total = reviews.size)
    }

    @PatchMapping("/qa-reviews/{id}")
    fun assignReview(
        @PathVariable id: String,
        @RequestBody request: AssignReviewRequest,
        servletRequest: HttpServletRequest,
    ): QAReviewAssignResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "qa.review.write")
        assignQAReviewUseCase.execute(id, request.assigneeId)
        return QAReviewAssignResponse(qaReviewId = id, assigneeId = request.assigneeId)
    }

    private fun requireAuthorized(session: AdminSessionSnapshot, actionCode: String) {
        try {
            adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode))
        } catch (exception: AdminAuthorizationException) {
            val status = when (exception.reason) {
                AuthorizationFailureReason.ACTION_FORBIDDEN -> HttpStatus.FORBIDDEN
                AuthorizationFailureReason.SCOPE_FORBIDDEN -> HttpStatus.FORBIDDEN
            }
            throw ResponseStatusException(status, exception.message, exception)
        }
    }
}

data class CreateQAReviewRequest(
    @field:NotBlank val questionId: String,
    @field:NotBlank val reviewStatus: String,
    val rootCauseCode: String?,
    val actionType: String?,
    val actionTargetId: String?,
    val reviewComment: String?,
    val assigneeId: String? = null,
)

data class AssignReviewRequest(val assigneeId: String?)

data class QAReviewCreateResponse(val qaReviewId: String, val questionId: String, val reviewStatus: String)
data class QAReviewAssignResponse(val qaReviewId: String, val assigneeId: String?)
data class QAReviewListResponse(val items: List<QAReviewResponse>, val total: Int)

data class QAReviewResponse(
    val id: String, val questionId: String, val reviewStatus: String,
    val rootCauseCode: String?, val actionType: String?,
    val actionTargetId: String?, val reviewComment: String?,
    val reviewerId: String, val assigneeId: String?, val reviewedAt: Instant,
)

private fun QAReviewSummary.toResponse() = QAReviewResponse(
    id = id, questionId = questionId, reviewStatus = reviewStatus.toApiValue(),
    rootCauseCode = rootCauseCode?.toApiValue(), actionType = actionType?.toApiValue(),
    actionTargetId = actionTargetId, reviewComment = reviewComment,
    reviewerId = reviewerId, assigneeId = assigneeId, reviewedAt = reviewedAt,
)

private fun String.toReviewStatus(): QAReviewStatus =
    when (this) {
        "pending" -> QAReviewStatus.PENDING
        "confirmed_issue" -> QAReviewStatus.CONFIRMED_ISSUE
        "false_alarm" -> QAReviewStatus.FALSE_ALARM
        "resolved" -> QAReviewStatus.RESOLVED
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review_status: $this")
    }

private fun String.toRootCauseCode(): RootCauseCode =
    when (this) {
        "missing_document" -> RootCauseCode.MISSING_DOCUMENT
        "stale_document" -> RootCauseCode.STALE_DOCUMENT
        "bad_chunking" -> RootCauseCode.BAD_CHUNKING
        "retrieval_failure" -> RootCauseCode.RETRIEVAL_FAILURE
        "generation_error" -> RootCauseCode.GENERATION_ERROR
        "policy_block" -> RootCauseCode.POLICY_BLOCK
        "unclear_question" -> RootCauseCode.UNCLEAR_QUESTION
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid root_cause_code: $this")
    }

private fun String.toActionType(): ActionType =
    when (this) {
        "faq_create" -> ActionType.FAQ_CREATE
        "document_fix_request" -> ActionType.DOCUMENT_FIX_REQUEST
        "reindex_request" -> ActionType.REINDEX_REQUEST
        "ops_issue" -> ActionType.OPS_ISSUE
        "no_action" -> ActionType.NO_ACTION
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action_type: $this")
    }

private fun QAReviewStatus.toApiValue() = when (this) {
    QAReviewStatus.PENDING -> "pending"; QAReviewStatus.CONFIRMED_ISSUE -> "confirmed_issue"
    QAReviewStatus.FALSE_ALARM -> "false_alarm"; QAReviewStatus.RESOLVED -> "resolved"
}

private fun RootCauseCode.toApiValue() = when (this) {
    RootCauseCode.MISSING_DOCUMENT -> "missing_document"; RootCauseCode.STALE_DOCUMENT -> "stale_document"
    RootCauseCode.BAD_CHUNKING -> "bad_chunking"; RootCauseCode.RETRIEVAL_FAILURE -> "retrieval_failure"
    RootCauseCode.GENERATION_ERROR -> "generation_error"; RootCauseCode.POLICY_BLOCK -> "policy_block"
    RootCauseCode.UNCLEAR_QUESTION -> "unclear_question"
}

private fun ActionType.toApiValue() = when (this) {
    ActionType.FAQ_CREATE -> "faq_create"; ActionType.DOCUMENT_FIX_REQUEST -> "document_fix_request"
    ActionType.REINDEX_REQUEST -> "reindex_request"; ActionType.OPS_ISSUE -> "ops_issue"
    ActionType.NO_ACTION -> "no_action"
}
