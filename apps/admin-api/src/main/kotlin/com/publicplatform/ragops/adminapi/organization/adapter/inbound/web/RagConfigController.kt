package com.publicplatform.ragops.adminapi.organization.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.GetRagConfigUseCase
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.RollbackRagConfigCommand
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.SaveRagConfigCommand
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.SaveRagConfigUseCase
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfigVersion
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

@RestController
@RequestMapping("/admin/organizations/{orgId}/rag-config")
class RagConfigController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val getRagConfigUseCase: GetRagConfigUseCase,
    private val saveRagConfigUseCase: SaveRagConfigUseCase,
) {
    @GetMapping
    fun getRagConfig(
        @PathVariable orgId: String,
        request: HttpServletRequest,
    ): RagConfigResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode = "organization.read"))
        val config = getRagConfigUseCase.getRagConfig(orgId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "RAG 설정을 찾을 수 없습니다: $orgId")
        return config.toResponse()
    }

    @PutMapping
    fun saveRagConfig(
        @PathVariable orgId: String,
        @RequestBody body: SaveRagConfigRequest,
        request: HttpServletRequest,
    ): RagConfigResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode = "organization.update"))
        val command = SaveRagConfigCommand(
            organizationId = orgId,
            systemPrompt = body.systemPrompt,
            tone = body.tone,
            topK = body.topK,
            similarityThreshold = body.similarityThreshold,
            rerankerEnabled = body.rerankerEnabled,
            llmModel = body.llmModel,
            llmTemperature = body.llmTemperature,
            llmMaxTokens = body.llmMaxTokens,
            changeNote = body.changeNote,
            changedBy = session.user.id,
        )
        return saveRagConfigUseCase.saveRagConfig(command).toResponse()
    }

    @GetMapping("/versions")
    fun listVersions(
        @PathVariable orgId: String,
        request: HttpServletRequest,
    ): RagConfigVersionListResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode = "organization.read"))
        val versions = getRagConfigUseCase.listVersions(orgId).map { it.toResponse() }
        return RagConfigVersionListResponse(items = versions, total = versions.size)
    }

    @PostMapping("/rollback/{version}")
    @ResponseStatus(HttpStatus.OK)
    fun rollback(
        @PathVariable orgId: String,
        @PathVariable version: Int,
        request: HttpServletRequest,
    ): RagConfigResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode = "organization.update"))
        val command = RollbackRagConfigCommand(
            organizationId = orgId,
            targetVersion = version,
            changedBy = session.user.id,
        )
        return saveRagConfigUseCase.rollback(command).toResponse()
    }
}

data class SaveRagConfigRequest(
    val systemPrompt: String,
    val tone: String = "formal",
    val topK: Int = 10,
    val similarityThreshold: BigDecimal = BigDecimal("0.700"),
    val rerankerEnabled: Boolean = false,
    val llmModel: String = "qwen2.5:7b",
    val llmTemperature: BigDecimal = BigDecimal("0.30"),
    val llmMaxTokens: Int = 500,
    val changeNote: String? = null,
)

data class RagConfigResponse(
    val id: String,
    val organizationId: String,
    val systemPrompt: String,
    val tone: String,
    val topK: Int,
    val similarityThreshold: BigDecimal,
    val rerankerEnabled: Boolean,
    val llmModel: String,
    val llmTemperature: BigDecimal,
    val llmMaxTokens: Int,
    val version: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class RagConfigVersionResponse(
    val id: String,
    val organizationId: String,
    val version: Int,
    val systemPrompt: String,
    val tone: String,
    val topK: Int,
    val similarityThreshold: BigDecimal,
    val rerankerEnabled: Boolean,
    val llmModel: String,
    val llmTemperature: BigDecimal,
    val llmMaxTokens: Int,
    val changeNote: String?,
    val changedBy: String?,
    val createdAt: Instant,
)

data class RagConfigVersionListResponse(
    val items: List<RagConfigVersionResponse>,
    val total: Int,
)

private fun RagConfig.toResponse() = RagConfigResponse(
    id = id,
    organizationId = organizationId,
    systemPrompt = systemPrompt,
    tone = tone,
    topK = topK,
    similarityThreshold = similarityThreshold,
    rerankerEnabled = rerankerEnabled,
    llmModel = llmModel,
    llmTemperature = llmTemperature,
    llmMaxTokens = llmMaxTokens,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RagConfigVersion.toResponse() = RagConfigVersionResponse(
    id = id,
    organizationId = organizationId,
    version = version,
    systemPrompt = systemPrompt,
    tone = tone,
    topK = topK,
    similarityThreshold = similarityThreshold,
    rerankerEnabled = rerankerEnabled,
    llmModel = llmModel,
    llmTemperature = llmTemperature,
    llmMaxTokens = llmMaxTokens,
    changeNote = changeNote,
    changedBy = changedBy,
    createdAt = createdAt,
)
