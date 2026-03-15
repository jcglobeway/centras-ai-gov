package com.publicplatform.ragops.chatruntime

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaAnswerRepository : JpaRepository<AnswerEntity, String> {
    fun findByQuestionId(questionId: String): AnswerEntity?
}
