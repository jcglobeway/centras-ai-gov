package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.CreateQuestionUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.RecordAnswerPort
import com.publicplatform.ragops.chatruntime.application.port.out.RecordQuestionPort
import com.publicplatform.ragops.chatruntime.application.port.out.RagOrchestrationPort
import com.publicplatform.ragops.chatruntime.domain.AnswerStatus
import com.publicplatform.ragops.chatruntime.domain.CreateAnswerCommand
import com.publicplatform.ragops.chatruntime.domain.CreateQuestionCommand
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary

/**
 * 질문 생성 유스케이스 구현체.
 *
 * 질문 저장 → RAG 오케스트레이터 호출 → 답변 저장 순으로 흐름을 조율한다.
 * 비즈니스 결정은 내리지 않으며 각 outbound port에 위임한다.
 *
 * RagOrchestrationPort가 null을 반환하면(disabled 또는 오류) 답변 저장 단계를 건너뛴다.
 */
open class CreateQuestionService(
    private val questionWriter: RecordQuestionPort,
    private val answerWriter: RecordAnswerPort,
    private val ragOrchestrationPort: RagOrchestrationPort,
) : CreateQuestionUseCase {

    override fun execute(command: CreateQuestionCommand): QuestionSummary {
        val createdQuestion = questionWriter.createQuestion(command)

        val ragResult = ragOrchestrationPort.generateAnswer(
            questionId = createdQuestion.id,
            questionText = createdQuestion.questionText,
            organizationId = createdQuestion.organizationId,
            serviceId = createdQuestion.serviceId,
        )

        if (ragResult != null) {
            answerWriter.createAnswer(
                CreateAnswerCommand(
                    questionId = createdQuestion.id,
                    answerText = ragResult.answerText,
                    answerStatus = ragResult.answerStatus,
                    responseTimeMs = ragResult.responseTimeMs,
                    citationCount = ragResult.citationCount,
                    fallbackReasonCode = ragResult.fallbackReasonCode,
                    modelName = ragResult.modelName,
                    providerName = ragResult.providerName,
                    inputTokens = ragResult.inputTokens,
                    outputTokens = ragResult.outputTokens,
                    totalTokens = ragResult.totalTokens,
                    estimatedCostUsd = ragResult.estimatedCostUsd,
                    finishReason = ragResult.finishReason,
                ),
            )
        }

        return createdQuestion
    }

}
