package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.LoadCorrectionPort
import com.publicplatform.ragops.chatruntime.domain.AnswerCorrectionSummary
import com.publicplatform.ragops.chatruntime.domain.CorrectionScope

open class LoadCorrectionPortAdapter(
    private val jpaRepository: JpaAnswerCorrectionRepository,
) : LoadCorrectionPort {

    override fun listCorrections(scope: CorrectionScope): List<AnswerCorrectionSummary> =
        if (scope.globalAccess) {
            jpaRepository.findAllByOrderByCreatedAtDesc()
        } else {
            jpaRepository.findByOrganizationIdInOrderByCreatedAtDesc(scope.organizationIds)
        }.map { it.toSummary() }
}
