package com.publicplatform.ragops.ingestionops.application.service

import com.publicplatform.ragops.ingestionops.application.port.`in`.ListIngestionUseCase
import com.publicplatform.ragops.ingestionops.application.port.out.LoadCrawlSourcePort
import com.publicplatform.ragops.ingestionops.application.port.out.LoadIngestionJobPort
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope

/**
 * 인제스션 관련 목록 조회 유스케이스 구현체.
 *
 * 크롤 소스와 인제스션 잡 목록 조회를 각 Reader에 위임한다.
 */
open class ListIngestionService(
    private val crawlSourceReader: LoadCrawlSourcePort,
    private val ingestionJobReader: LoadIngestionJobPort,
) : ListIngestionUseCase {

    override fun listSources(scope: IngestionScope): List<CrawlSourceSummary> =
        crawlSourceReader.listSources(scope)

    override fun listJobs(scope: IngestionScope): List<IngestionJobSummary> =
        ingestionJobReader.listJobs(scope)
}
