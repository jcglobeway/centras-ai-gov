package com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRagasEvaluationRepository : JpaRepository<RagasEvaluationEntity, String> {
    fun findByQuestionId(questionId: String, pageable: Pageable): List<RagasEvaluationEntity>
    fun countByQuestionId(questionId: String): Long
}
