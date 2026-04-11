package com.publicplatform.ragops.adminapi.redteam.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationException
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.identityaccess.domain.AuthorizationFailureReason
import com.publicplatform.ragops.redteam.application.port.`in`.ManageRedteamCaseUseCase
import com.publicplatform.ragops.redteam.domain.CreateRedteamCaseCommand
import com.publicplatform.ragops.redteam.domain.RedteamCategory
import com.publicplatform.ragops.redteam.domain.RedteamCaseSummary
import com.publicplatform.ragops.redteam.domain.RedteamExpectedBehavior
import com.publicplatform.ragops.redteam.domain.UpdateRedteamCaseCommand
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/admin/redteam")
class RedteamCaseController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val manageRedteamCaseUseCase: ManageRedteamCaseUseCase,
) {

    @PostMapping("/cases")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCase(
        @Valid @RequestBody request: CreateRedteamCaseRequest,
        servletRequest: HttpServletRequest,
    ): RedteamCaseResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "redteam.case.write")

        return try {
            manageRedteamCaseUseCase.createCase(
                CreateRedteamCaseCommand(
                    category = request.category.toCategory(),
                    title = request.title,
                    queryText = request.queryText,
                    expectedBehavior = request.expectedBehavior.toExpectedBehavior(),
                    createdBy = session.user.id,
                ),
            ).toResponse()
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        } catch (e: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
    }

    @GetMapping("/cases")
    fun listCases(
        servletRequest: HttpServletRequest,
    ): RedteamCaseListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "redteam.case.read")

        val cases = manageRedteamCaseUseCase.listCases()
        return RedteamCaseListResponse(cases = cases.map { it.toResponse() }, total = cases.size)
    }

    @PutMapping("/cases/{id}")
    fun updateCase(
        @PathVariable id: String,
        @RequestBody request: UpdateRedteamCaseRequest,
        servletRequest: HttpServletRequest,
    ): RedteamCaseResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "redteam.case.write")

        return try {
            manageRedteamCaseUseCase.updateCase(
                id,
                UpdateRedteamCaseCommand(
                    title = request.title,
                    queryText = request.queryText,
                    expectedBehavior = request.expectedBehavior?.toExpectedBehavior(),
                    isActive = request.isActive,
                ),
            ).toResponse()
        } catch (e: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
    }

    @DeleteMapping("/cases/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCase(
        @PathVariable id: String,
        servletRequest: HttpServletRequest,
    ) {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        requireAuthorized(session, "redteam.case.delete")
        try {
            manageRedteamCaseUseCase.deleteCase(id)
        } catch (e: NoSuchElementException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
    }

    private fun requireAuthorized(session: com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot, actionCode: String) {
        try {
            adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode))
        } catch (e: AdminAuthorizationException) {
            val status = when (e.reason) {
                AuthorizationFailureReason.ACTION_FORBIDDEN -> HttpStatus.FORBIDDEN
                AuthorizationFailureReason.SCOPE_FORBIDDEN -> HttpStatus.FORBIDDEN
            }
            throw ResponseStatusException(status, e.message, e)
        }
    }
}

data class CreateRedteamCaseRequest(
    @field:NotBlank val category: String,
    @field:NotBlank val title: String,
    @field:NotBlank val queryText: String,
    @field:NotBlank val expectedBehavior: String,
)

data class UpdateRedteamCaseRequest(
    val title: String? = null,
    val queryText: String? = null,
    val expectedBehavior: String? = null,
    val isActive: Boolean? = null,
)

data class RedteamCaseResponse(
    val id: String,
    val category: String,
    val title: String,
    val queryText: String,
    val expectedBehavior: String,
    val isActive: Boolean,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class RedteamCaseListResponse(
    val cases: List<RedteamCaseResponse>,
    val total: Int,
)

fun RedteamCaseSummary.toResponse() = RedteamCaseResponse(
    id = id,
    category = category.name.lowercase(),
    title = title,
    queryText = queryText,
    expectedBehavior = expectedBehavior.name.lowercase(),
    isActive = isActive,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun String.toCategory(): RedteamCategory = when (this) {
    "pii_induction" -> RedteamCategory.PII_INDUCTION
    "out_of_domain" -> RedteamCategory.OUT_OF_DOMAIN
    "prompt_injection" -> RedteamCategory.PROMPT_INJECTION
    "harmful_content" -> RedteamCategory.HARMFUL_CONTENT
    else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category: $this")
}

fun String.toExpectedBehavior(): RedteamExpectedBehavior = when (this) {
    "defend" -> RedteamExpectedBehavior.DEFEND
    "detect" -> RedteamExpectedBehavior.DETECT
    else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid expectedBehavior: $this")
}
