package com.publicplatform.ragops.ingestionops.application.port.`in`

import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope

/**
 * 인제스션 관련 목록 조회 인바운드 포트.
 *
 * 크롤 소스와 인제스션 잡 목록을 기관 범위에 따라 필터링하여 반환한다.
 */
interface ListIngestionUseCase {
    fun listSources(scope: IngestionScope): List<CrawlSourceSummary>
    fun listJobs(scope: IngestionScope): List<IngestionJobSummary>
}
