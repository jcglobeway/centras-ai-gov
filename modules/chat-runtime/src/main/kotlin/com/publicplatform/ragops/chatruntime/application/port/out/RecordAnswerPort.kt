/**
 * RAG 답변 생성 결과 저장 아웃바운드 포트.
 *
 * RAG 오케스트레이터가 답변을 생성하면 이 포트를 통해 answers 테이블에 저장한다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import com.publicplatform.ragops.chatruntime.domain.CreateAnswerCommand

interface RecordAnswerPort {
    fun createAnswer(command: CreateAnswerCommand): AnswerSummary
}
