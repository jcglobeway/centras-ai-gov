/**
 * LoadCrawlSourcePort의 JPA 구현체.
 *
 * 기관 범위에 따라 크롤 소스 목록을 필터링하여 반환한다.
 */
package com.publicplatform.ragops.ingestionops.adapter.outbound.persistence

import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope
import com.publicplatform.ragops.ingestionops.application.port.out.LoadCrawlSourcePort

open class LoadCrawlSourcePortAdapter(
    private val jpaRepository: JpaCrawlSourceRepository,
) : LoadCrawlSourcePort {

    override fun listSources(scope: IngestionScope): List<CrawlSourceSummary> {
        val allSources = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) allSources
        else allSources.filter { it.organizationId in scope.organizationIds }
    }
}
