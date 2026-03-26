package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.FaqCandidate

interface ListFaqCandidatesUseCase {
    fun list(organizationId: String, threshold: Double): List<FaqCandidate>
}
