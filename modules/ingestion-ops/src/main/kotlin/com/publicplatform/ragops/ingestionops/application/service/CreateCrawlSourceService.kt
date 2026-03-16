package com.publicplatform.ragops.ingestionops.application.service

import com.publicplatform.ragops.ingestionops.application.port.`in`.CreateCrawlSourceUseCase
import com.publicplatform.ragops.ingestionops.application.port.out.SaveCrawlSourcePort
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.CreateCrawlSourceCommand

/**
 * 크롤 소스 생성 유스케이스 구현체.
 *
 * SaveCrawlSourcePort에 위임하여 크롤 대상을 저장한다.
 */
open class CreateCrawlSourceService(
    private val crawlSourceWriter: SaveCrawlSourcePort,
) : CreateCrawlSourceUseCase {

    override fun execute(command: CreateCrawlSourceCommand): CrawlSourceSummary =
        crawlSourceWriter.createSource(command)
}
