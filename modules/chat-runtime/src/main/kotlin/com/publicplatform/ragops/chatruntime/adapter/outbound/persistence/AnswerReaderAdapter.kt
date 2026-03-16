/**
 * LoadAnswerPort의 JPA 구현체.
 *
 * 질문 ID로 답변을 단건 조회한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import com.publicplatform.ragops.chatruntime.application.port.out.LoadAnswerPort

open class LoadAnswerPortAdapter(
    private val jpaRepository: JpaAnswerRepository,
) : LoadAnswerPort {

    override fun findByQuestionId(questionId: String): AnswerSummary? {
        return jpaRepository.findByQuestionId(questionId)?.toSummary()
    }
}
