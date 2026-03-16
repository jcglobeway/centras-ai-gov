package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.ListQuestionsUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadQuestionPort
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary

/**
 * 질문 목록 조회 유스케이스 구현체.
 *
 * LoadQuestionPort에 위임하여 전체 목록과 미해결 큐를 반환한다.
 */
open class ListQuestionsService(
    private val questionReader: LoadQuestionPort,
) : ListQuestionsUseCase {

    override fun listAll(scope: ChatScope): List<QuestionSummary> =
        questionReader.listQuestions(scope)

    override fun listUnresolved(scope: ChatScope): List<QuestionSummary> =
        questionReader.listUnresolvedQuestions(scope)
}
