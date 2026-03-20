package com.publicplatform.ragops.adminapi.chatruntime.adapter.outbound.http

import com.publicplatform.ragops.chatruntime.application.port.out.RagOrchestrationPort
import com.publicplatform.ragops.chatruntime.domain.RagAnswerResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

/**
 * RAG 오케스트레이터 HTTP 클라이언트 (fallback용).
 *
 * Spring AI가 기본 답변 생성을 담당하며, 이 클라이언트는 fallback으로만 사용된다.
 * rag.orchestrator.enabled=false이면 null을 반환해 답변 생성을 건너뛴다.
 */
class RagOrchestratorClient(
    @Value("\${rag.orchestrator.url:http://localhost:8090}")
    private val ragOrchestratorUrl: String,
    @Value("\${rag.orchestrator.enabled:false}")
    private val ragOrchestratorEnabled: Boolean,
    restTemplateBuilder: RestTemplateBuilder,
) : RagOrchestrationPort {

    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    override fun generateAnswer(
        questionId: String,
        questionText: String,
        organizationId: String,
        serviceId: String,
    ): RagAnswerResult? {
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

            response.body?.toRagAnswerResult()
        } catch (e: Exception) {
            null
        }
    }
}

private data class GenerateAnswerResult(
    val question_id: String,
    val answer_text: String,
    val answer_status: String,
    val citation_count: Int,
    val response_time_ms: Int,
    val fallback_reason_code: String?,
)

private fun GenerateAnswerResult.toRagAnswerResult() = RagAnswerResult(
    answerText = answer_text,
    answerStatus = answer_status,
    responseTimeMs = response_time_ms,
    citationCount = citation_count,
    fallbackReasonCode = fallback_reason_code,
)
