/**
 * 질문 목록 조회 아웃바운드 포트.
 *
 * listUnresolvedQuestions()는 QA 미해결 큐 조건(fallback/no_answer 또는 confirmed_issue)에 해당하는
 * 질문만 반환한다. 복잡한 필터는 JPA Native SQL로 처리하여 모듈 간 순환 의존을 방지한다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary
import com.publicplatform.ragops.chatruntime.domain.UnresolvedQuestionSummary

interface LoadQuestionPort {
    fun listQuestions(scope: ChatScope): List<QuestionSummary>
    fun listUnresolvedQuestions(scope: ChatScope): List<UnresolvedQuestionSummary>
}
