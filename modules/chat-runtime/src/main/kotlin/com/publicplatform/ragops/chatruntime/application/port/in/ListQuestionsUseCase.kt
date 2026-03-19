package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary

/**
 * 질문 목록 조회 인바운드 포트.
 *
 * 전체 질문 목록 및 미해결 질문 큐를 제공한다.
 * 미해결 큐는 fallback/no_answer/confirmed_issue 상태의 질문을 포함한다.
 */
interface ListQuestionsUseCase {
    fun listAll(scope: ChatScope, from: String? = null, to: String? = null): List<QuestionSummary>
    fun listUnresolved(scope: ChatScope, from: String? = null, to: String? = null): List<QuestionSummary>
}
