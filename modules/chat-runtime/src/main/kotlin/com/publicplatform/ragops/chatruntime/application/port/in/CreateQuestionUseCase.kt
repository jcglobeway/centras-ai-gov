package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.CreateQuestionCommand
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary

/**
 * 질문 생성 인바운드 포트.
 *
 * 시민 채팅에서 질문이 수신될 때 호출된다.
 * 구현체는 질문 저장 → RAG 오케스트레이터 호출 → 답변 저장 흐름을 조율한다.
 */
interface CreateQuestionUseCase {
    fun execute(command: CreateQuestionCommand): QuestionSummary
}
