package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.ListQuestionsUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadQuestionPort
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 질문 목록 조회 유스케이스 구현체.
 *
 * LoadQuestionPort에 위임하여 전체 목록과 미해결 큐를 반환한다.
 * 날짜 필터는 서비스 레이어에서 처리하여 Controller 책임을 분리한다.
 */
open class ListQuestionsService(
    private val questionReader: LoadQuestionPort,
) : ListQuestionsUseCase {

    override fun listAll(scope: ChatScope, from: String?, to: String?): List<QuestionSummary> =
        questionReader.listQuestions(scope).filterByDateRange(from, to)

    override fun listUnresolved(scope: ChatScope, from: String?, to: String?): List<QuestionSummary> =
        questionReader.listUnresolvedQuestions(scope).filterByDateRange(from, to)

    private fun List<QuestionSummary>.filterByDateRange(from: String?, to: String?): List<QuestionSummary> {
        val fromInst = from?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant() }
        val toInst = to?.let { LocalDate.parse(it).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() }
        return filter {
            (fromInst == null || it.createdAt >= fromInst) && (toInst == null || it.createdAt < toInst)
        }
    }
}
