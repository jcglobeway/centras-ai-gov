package com.publicplatform.ragops.chatruntime

open class QuestionReaderAdapter(
    private val jpaRepository: JpaQuestionRepository,
) : QuestionReader {

    override fun listQuestions(scope: ChatScope): List<QuestionSummary> {
        val allQuestions = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) {
            allQuestions
        } else {
            allQuestions.filter { it.organizationId in scope.organizationIds }
        }
    }

    override fun listUnresolvedQuestions(scope: ChatScope): List<QuestionSummary> {
        val unresolvedQuestions = jpaRepository.findUnresolvedQuestions().map { it.toSummary() }
        return if (scope.globalAccess) {
            unresolvedQuestions
        } else {
            unresolvedQuestions.filter { it.organizationId in scope.organizationIds }
        }
    }
}
