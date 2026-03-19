/**
 * LoadFeedbackPort의 JPA 구현체.
 *
 * 기관 범위에 따라 피드백 목록을 필터링하여 반환한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.FeedbackScope
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary
import com.publicplatform.ragops.chatruntime.application.port.out.LoadFeedbackPort

open class LoadFeedbackPortAdapter(
    private val jpaRepository: JpaFeedbackRepository,
) : LoadFeedbackPort {

    override fun listFeedbacks(scope: FeedbackScope): List<FeedbackSummary> =
        if (scope.globalAccess) {
            jpaRepository.findAllByOrderBySubmittedAtDesc()
        } else {
            jpaRepository.findByOrganizationIdInOrderBySubmittedAtDesc(scope.organizationIds)
        }.map { it.toSummary() }
}
