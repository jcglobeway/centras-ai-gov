package com.publicplatform.ragops.ingestionops.adapter.outbound.persistence

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope
import com.publicplatform.ragops.ingestionops.application.port.out.LoadIngestionJobPort

open class LoadIngestionJobPortAdapter(
    private val jpaRepository: JpaIngestionJobRepository,
) : LoadIngestionJobPort {

    override fun listJobs(scope: IngestionScope): List<IngestionJobSummary> {
        val allJobs = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) allJobs
        else allJobs.filter { it.organizationId in scope.organizationIds }
    }
}
