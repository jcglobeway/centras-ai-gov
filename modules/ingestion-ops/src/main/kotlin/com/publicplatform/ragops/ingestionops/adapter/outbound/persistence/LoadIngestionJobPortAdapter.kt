/**
 * LoadIngestionJobPort의 JPA 구현체.
 *
 * 기관 범위에 따라 인제스션 잡 목록을 필터링하여 반환한다.
 */
package com.publicplatform.ragops.ingestionops.adapter.outbound.persistence

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope
import com.publicplatform.ragops.ingestionops.application.port.out.LoadIngestionJobPort
import org.springframework.data.domain.Sort

open class LoadIngestionJobPortAdapter(
    private val jpaRepository: JpaIngestionJobRepository,
) : LoadIngestionJobPort {

    override fun listJobs(scope: IngestionScope): List<IngestionJobSummary> {
        val allJobs = jpaRepository.findAll(Sort.by("id")).map { it.toSummary() }
        return if (scope.globalAccess) allJobs
        else allJobs.filter { it.organizationId in scope.organizationIds }
    }
}
