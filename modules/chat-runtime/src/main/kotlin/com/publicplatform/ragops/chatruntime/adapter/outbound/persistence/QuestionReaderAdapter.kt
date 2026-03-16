package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary
import com.publicplatform.ragops.chatruntime.application.port.out.LoadQuestionPort

open class LoadQuestionPortAdapter(
    private val jpaRepository: JpaQuestionRepository,
) : LoadQuestionPort {

    override fun listQuestions(scope: ChatScope): List<QuestionSummary> {
        val allQuestions = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) allQuestions
        else allQuestions.filter { it.organizationId in scope.organizationIds }
    }

    override fun listUnresolvedQuestions(scope: ChatScope): List<QuestionSummary> {
        val unresolvedQuestions = jpaRepository.findUnresolvedQuestions().map { it.toSummary() }
        return if (scope.globalAccess) unresolvedQuestions
        else unresolvedQuestions.filter { it.organizationId in scope.organizationIds }
    }
}
