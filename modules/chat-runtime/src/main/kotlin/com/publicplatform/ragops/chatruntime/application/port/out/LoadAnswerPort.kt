/**
 * 질문별 답변 조회 아웃바운드 포트.
 *
 * 질문 1개에 답변 0~1개 관계이므로 nullable로 반환한다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.AnswerSummary

interface LoadAnswerPort {
    fun findByQuestionId(questionId: String): AnswerSummary?
}
