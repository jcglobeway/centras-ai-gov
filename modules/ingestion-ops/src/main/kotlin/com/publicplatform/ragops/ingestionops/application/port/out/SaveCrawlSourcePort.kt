/**
 * 크롤 소스 생성 아웃바운드 포트.
 *
 * 새 크롤 소스를 등록하며 ID는 어댑터에서 "crawl_src_" 접두사로 생성한다.
 */
package com.publicplatform.ragops.ingestionops.application.port.out

import com.publicplatform.ragops.ingestionops.domain.CreateCrawlSourceCommand
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary

interface SaveCrawlSourcePort {
    fun createSource(command: CreateCrawlSourceCommand): CrawlSourceSummary
}
