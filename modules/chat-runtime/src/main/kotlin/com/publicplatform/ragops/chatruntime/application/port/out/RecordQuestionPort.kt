/**
 * 질문 생성 아웃바운드 포트.
 *
 * 시민이 챗봇에 질문을 제출하면 이 포트를 통해 영속화한다.
 * ID는 어댑터에서 UUID 기반으로 생성하며 "question_" 접두사를 사용한다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.CreateQuestionCommand
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary

interface RecordQuestionPort {
    fun createQuestion(command: CreateQuestionCommand): QuestionSummary
}
