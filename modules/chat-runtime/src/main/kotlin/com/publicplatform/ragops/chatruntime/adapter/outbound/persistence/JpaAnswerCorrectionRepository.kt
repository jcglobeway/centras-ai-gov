package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaAnswerCorrectionRepository : JpaRepository<AnswerCorrectionEntity, String> {
    fun findByOrganizationIdInOrderByCreatedAtDesc(organizationIds: Collection<String>): List<AnswerCorrectionEntity>
    fun findAllByOrderByCreatedAtDesc(): List<AnswerCorrectionEntity>
}
