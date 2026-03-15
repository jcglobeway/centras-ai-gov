package com.publicplatform.ragops.chatruntime

open class AnswerReaderAdapter(
    private val jpaRepository: JpaAnswerRepository,
) : AnswerReader {

    override fun findByQuestionId(questionId: String): AnswerSummary? {
        return jpaRepository.findByQuestionId(questionId)?.toSummary()
    }
}
