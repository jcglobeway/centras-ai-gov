package com.publicplatform.ragops.adminapi.chatruntime.adapter.outbound.ai

import com.publicplatform.ragops.chatruntime.application.port.out.RagOrchestrationPort
import com.publicplatform.ragops.chatruntime.domain.AnswerStatus
import com.publicplatform.ragops.chatruntime.domain.RagAnswerResult
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt

/**
 * Spring AI 기반 답변 생성 서비스.
 *
 * ChatModel(OpenAI 또는 Ollama)을 사용해 질문에 대한 답변을 생성한다.
 * ChatResponse에서 토큰 사용량·비용·모델명·finish_reason을 추출해 RagAnswerResult에 포함한다.
 * 예외 발생 시 null을 반환해 fallback으로 처리된다.
 */
open class SpringAiAnswerService(
    private val chatModel: ChatModel,
    private val providerName: String,
) : RagOrchestrationPort {

    companion object {
        // 더 구체적인 prefix가 앞에 와야 startsWith 매칭이 올바르게 동작한다.
        // "gpt-4o-mini"가 "gpt-4o" 뒤에 오면 mini 모델이 gpt-4o 요금으로 과금된다.
        private val PRICING = mapOf(
            "gpt-5-latest" to (1.25 to 7.50),
            "gpt-4o-mini" to (0.15 to 0.60),
            "gpt-4o" to (2.50 to 10.00),
        )
    }

    override fun generateAnswer(
        questionId: String,
        questionText: String,
        organizationId: String,
        serviceId: String,
    ): RagAnswerResult? {
        return try {
            val startMs = System.currentTimeMillis()
            val prompt = Prompt(
                "당신은 공공기관 민원 안내 챗봇입니다. 다음 질문에 정확하고 친절하게 답변하세요.\n\n질문: $questionText"
            )
            val response = chatModel.call(prompt)
            val answerText = response.result?.output?.text ?: "[답변 생성 실패]"
            val responseTimeMs = (System.currentTimeMillis() - startMs).toInt()

            val usage = response.metadata?.usage
            val inputTokens = usage?.promptTokens?.toInt()
            val outputTokens = usage?.completionTokens?.toInt()
            val totalTokens = usage?.totalTokens?.toInt()
            val modelName = response.metadata?.model ?: "unknown"
            val finishReason = response.result?.metadata?.finishReason

            RagAnswerResult(
                answerText = answerText,
                answerStatus = AnswerStatus.ANSWERED.name.lowercase(),
                responseTimeMs = responseTimeMs,
                citationCount = 0,
                fallbackReasonCode = null,
                modelName = modelName,
                providerName = providerName,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                totalTokens = totalTokens,
                estimatedCostUsd = estimateCostUsd(modelName, inputTokens, outputTokens),
                finishReason = finishReason,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun estimateCostUsd(model: String?, input: Int?, output: Int?): Double? {
        val (inRate, outRate) = PRICING.entries
            .firstOrNull { model?.startsWith(it.key) == true }?.value ?: return null
        return ((input ?: 0) * inRate + (output ?: 0) * outRate) / 1_000_000.0
    }
}
