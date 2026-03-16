package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import com.publicplatform.ragops.chatruntime.domain.CreateAnswerCommand

/**
 * 답변 수동 생성 인바운드 포트.
 *
 * RAG 오케스트레이터가 아닌 관리자가 직접 답변을 입력할 때 사용한다.
 */
interface CreateAnswerUseCase {
    fun execute(command: CreateAnswerCommand): AnswerSummary
}
