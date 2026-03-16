package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaFeedbackRepository : JpaRepository<FeedbackEntity, String> {
    fun findByOrganizationIdInOrderBySubmittedAtDesc(organizationIds: Collection<String>): List<FeedbackEntity>
    fun findAllByOrderBySubmittedAtDesc(): List<FeedbackEntity>
}
