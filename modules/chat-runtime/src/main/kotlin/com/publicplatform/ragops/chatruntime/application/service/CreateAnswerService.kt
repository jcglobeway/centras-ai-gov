package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.CreateAnswerUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.RecordAnswerPort
import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import com.publicplatform.ragops.chatruntime.domain.CreateAnswerCommand

/**
 * 답변 수동 생성 유스케이스 구현체.
 *
 * 관리자가 직접 답변을 입력하는 경우 RecordAnswerPort에 위임한다.
 */
open class CreateAnswerService(
    private val answerWriter: RecordAnswerPort,
) : CreateAnswerUseCase {

    override fun execute(command: CreateAnswerCommand): AnswerSummary =
        answerWriter.createAnswer(command)
}
