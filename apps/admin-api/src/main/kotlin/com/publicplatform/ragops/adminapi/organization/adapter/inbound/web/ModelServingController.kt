package com.publicplatform.ragops.adminapi.organization.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@RestController
@RequestMapping("/admin/model-serving")
class ModelServingController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${rag.orchestrator.url:http://localhost:8090}") private val ragOrchestratorUrl: String,
    @Value("\${rag.orchestrator.enabled:false}") private val ragOrchestratorEnabled: Boolean,
) {
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    @GetMapping("/status")
    fun getModelServingStatus(request: HttpServletRequest): ModelServingStatusResponse {
        val session = adminRequestSessionResolver.resolve(request)
        adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck(actionCode = "organization.read"))

        if (!ragOrchestratorEnabled) {
            return ModelServingStatusResponse(
                orchestratorStatus = "disabled",
                models = emptyList(),
            )
        }

        return try {
            val health = restTemplate.getForObject<Map<*, *>>("$ragOrchestratorUrl/healthz")
            val tags = fetchOllamaTags()
            ModelServingStatusResponse(
                orchestratorStatus = health?.get("status")?.toString() ?: "unknown",
                models = tags,
            )
        } catch (e: Exception) {
            ModelServingStatusResponse(
                orchestratorStatus = "error",
                models = emptyList(),
            )
        }
    }

    private fun fetchOllamaTags(): List<ModelInfo> {
        return try {
            val result = restTemplate.getForObject<Map<*, *>>("$ragOrchestratorUrl/ollama-tags")
            @Suppress("UNCHECKED_CAST")
            val models = result?.get("models") as? List<Map<*, *>> ?: emptyList()
            models.map { m ->
                ModelInfo(
                    name = m["name"]?.toString() ?: "unknown",
                    status = "ok",
                    version = m["details"]?.let { (it as? Map<*, *>)?.get("family")?.toString() },
                    latencyMs = null,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class ModelServingStatusResponse(
    val orchestratorStatus: String,
    val models: List<ModelInfo>,
)

data class ModelInfo(
    val name: String,
    val status: String,
    val version: String?,
    val latencyMs: Long?,
)
