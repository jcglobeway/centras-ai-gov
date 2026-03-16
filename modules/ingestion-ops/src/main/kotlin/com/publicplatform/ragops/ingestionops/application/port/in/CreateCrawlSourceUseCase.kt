package com.publicplatform.ragops.ingestionops.application.port.`in`

import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.CreateCrawlSourceCommand

/**
 * 크롤 소스 생성 인바운드 포트.
 *
 * 관리자가 새 크롤 대상(웹사이트, 사이트맵, 파일 드롭)을 등록할 때 호출된다.
 */
interface CreateCrawlSourceUseCase {
    fun execute(command: CreateCrawlSourceCommand): CrawlSourceSummary
}
