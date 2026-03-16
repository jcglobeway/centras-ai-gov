package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaAnswerRepository : JpaRepository<AnswerEntity, String> {
    fun findByQuestionId(questionId: String): AnswerEntity?
}
