package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.chatruntime.application.port.`in`.ManageCorrectionUseCase
import com.publicplatform.ragops.chatruntime.domain.AnswerCorrectionSummary
import com.publicplatform.ragops.chatruntime.domain.CorrectionScope
import com.publicplatform.ragops.chatruntime.domain.CreateCorrectionCommand
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
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
 * 답변 교정(Ground Truth) HTTP 인바운드 어댑터.
 *
 * 교정 생성 및 이력 조회를 ManageCorrectionUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class CorrectionController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val manageCorrectionUseCase: ManageCorrectionUseCase,
) {

    @PostMapping("/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCorrection(
        @Valid @RequestBody request: CreateCorrectionRequest,
        servletRequest: HttpServletRequest,
    ): CorrectionCreateResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)

        val firstOrgId = session.roleAssignments.mapNotNull { it.organizationId }.firstOrNull() ?: "unknown"

        val created = manageCorrectionUseCase.create(
            CreateCorrectionCommand(
                organizationId = request.organizationId ?: firstOrgId,
                serviceId = request.serviceId ?: "unknown",
                questionId = request.questionId,
                questionText = request.questionText ?: "",
                originalAnswerText = request.originalAnswerText,
                correctedAnswerText = request.correctedAnswerText,
                correctedBy = session.user.email,
                correctionReason = request.correctionReason,
            ),
        )

        return CorrectionCreateResponse(id = created.id, questionId = created.questionId)
    }

    @GetMapping("/corrections")
    fun listCorrections(servletRequest: HttpServletRequest): CorrectionListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val items = manageCorrectionUseCase.list(session.toScope()).map { it.toResponse() }
        return CorrectionListResponse(items = items, total = items.size)
    }
}

data class CreateCorrectionRequest(
    val organizationId: String?,
    val serviceId: String?,
    @field:NotBlank val questionId: String,
    val questionText: String?,
    val originalAnswerText: String?,
    @field:NotBlank val correctedAnswerText: String,
    val correctionReason: String?,
)

data class CorrectionCreateResponse(val id: String, val questionId: String)

data class CorrectionListResponse(val items: List<CorrectionResponse>, val total: Int)

data class CorrectionResponse(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val questionId: String,
    val questionText: String,
    val originalAnswerText: String?,
    val correctedAnswerText: String,
    val correctedBy: String,
    val correctionReason: String?,
    val createdAt: Instant,
)

private fun AnswerCorrectionSummary.toResponse() = CorrectionResponse(
    id = id,
    organizationId = organizationId,
    serviceId = serviceId,
    questionId = questionId,
    questionText = questionText,
    originalAnswerText = originalAnswerText,
    correctedAnswerText = correctedAnswerText,
    correctedBy = correctedBy,
    correctionReason = correctionReason,
    createdAt = createdAt,
)

private fun AdminSessionSnapshot.toScope() = CorrectionScope(
    organizationIds = roleAssignments.mapNotNull { it.organizationId }.toSet(),
    globalAccess = roleAssignments.any { it.organizationId == null },
)
