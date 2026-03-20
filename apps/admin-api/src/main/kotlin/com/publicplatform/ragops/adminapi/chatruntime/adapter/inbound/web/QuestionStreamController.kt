package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import org.springframework.ai.chat.model.StreamingChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors

/**
 * 질문 스트리밍 SSE 컨트롤러.
 *
 * GET /admin/questions/stream으로 question_text를 받아 Ollama StreamingChatModel을
 * 통해 토큰 단위 SSE 스트림을 반환한다.
 */
@RestController
@RequestMapping("/admin/questions")
class QuestionStreamController(
    private val streamingChatModel: StreamingChatModel?,
) {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAnswer(
        @RequestParam("question_text") questionText: String,
        @RequestParam("organization_id") organizationId: String,
        @RequestParam("service_id") serviceId: String,
    ): SseEmitter {
        val emitter = SseEmitter(60_000L)

        if (streamingChatModel == null) {
            emitter.completeWithError(IllegalStateException("Spring AI streaming not enabled"))
            return emitter
        }

        val promptText = "당신은 공공기관 민원 안내 챗봇입니다. 다음 질문에 정확하고 친절하게 답변하세요.\n\n질문: $questionText"

        executor.submit {
            try {
                val flux = streamingChatModel.stream(Prompt(promptText))
                flux.subscribe(
                    { chunk ->
                        val token = chunk.result?.output?.text ?: ""
                        if (token.isNotEmpty()) emitter.send(SseEmitter.event().data(token))
                    },
                    { error -> emitter.completeWithError(error) },
                    { emitter.complete() },
                )
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }
        return emitter
    }
}
