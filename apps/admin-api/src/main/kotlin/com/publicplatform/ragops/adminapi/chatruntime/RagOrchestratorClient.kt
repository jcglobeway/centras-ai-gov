package com.publicplatform.ragops.adminapi.chatruntime

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class RagOrchestratorClient(
    @Value("\${rag.orchestrator.url:http://localhost:8090}")
    private val ragOrchestratorUrl: String,
    @Value("\${rag.orchestrator.enabled:false}")
    private val ragOrchestratorEnabled: Boolean,
    restTemplateBuilder: RestTemplateBuilder,
) {
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    fun generateAnswer(
        questionId: String,
        questionText: String,
        organizationId: String,
        serviceId: String,
    ): GenerateAnswerResult? {
        if (!ragOrchestratorEnabled) {
            return null
        }

        return try {
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            val requestBody = mapOf(
                "question_id" to questionId,
                "question_text" to questionText,
                "organization_id" to organizationId,
                "service_id" to serviceId,
            )

            val request = HttpEntity(requestBody, headers)
            val response = restTemplate.postForEntity(
                "$ragOrchestratorUrl/generate",
                request,
                GenerateAnswerResult::class.java,
            )

            response.body
        } catch (e: Exception) {
            // RAG orchestrator 호출 실패 시 null 반환 (fallback)
            null
        }
    }
}

data class GenerateAnswerResult(
    val question_id: String,
    val answer_text: String,
    val answer_status: String,
    val citation_count: Int,
    val response_time_ms: Int,
    val fallback_reason_code: String?,
)
