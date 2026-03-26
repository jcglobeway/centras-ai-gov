package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.ListFaqCandidatesUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadFaqCandidatesPort
import com.publicplatform.ragops.chatruntime.domain.FaqCandidate

open class ListFaqCandidatesService(
    private val loadFaqCandidatesPort: LoadFaqCandidatesPort,
) : ListFaqCandidatesUseCase {

    override fun list(organizationId: String, threshold: Double): List<FaqCandidate> =
        loadFaqCandidatesPort.findFaqCandidates(organizationId, threshold)
}
