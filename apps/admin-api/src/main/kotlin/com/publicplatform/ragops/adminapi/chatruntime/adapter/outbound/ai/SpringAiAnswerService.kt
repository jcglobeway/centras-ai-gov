package com.publicplatform.ragops.adminapi.chatruntime.adapter.outbound.ai

import com.publicplatform.ragops.chatruntime.application.port.out.RagOrchestrationPort
import com.publicplatform.ragops.chatruntime.domain.RagAnswerResult
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt

/**
 * Spring AI 기반 답변 생성 서비스.
 *
 * Ollama ChatModel을 사용해 질문에 대한 답변을 생성한다.
 * enabled=false이면 null을 반환해 fallback으로 처리된다.
 */
open class SpringAiAnswerService(
    private val chatModel: ChatModel,
    private val enabled: Boolean,
) : RagOrchestrationPort {

    override fun generateAnswer(
        questionId: String,
        questionText: String,
        organizationId: String,
        serviceId: String,
    ): RagAnswerResult? {
        if (!enabled) return null

        return try {
            val startMs = System.currentTimeMillis()
            val prompt = Prompt(
                "당신은 공공기관 민원 안내 챗봇입니다. 다음 질문에 정확하고 친절하게 답변하세요.\n\n질문: $questionText"
            )
            val response = chatModel.call(prompt)
            val answerText = response.result?.output?.text ?: "[답변 생성 실패]"

            RagAnswerResult(
                answerText = answerText,
                answerStatus = "answered",
                responseTimeMs = (System.currentTimeMillis() - startMs).toInt(),
                citationCount = 0,
                fallbackReasonCode = null,
            )
        } catch (e: Exception) {
            null
        }
    }
}
