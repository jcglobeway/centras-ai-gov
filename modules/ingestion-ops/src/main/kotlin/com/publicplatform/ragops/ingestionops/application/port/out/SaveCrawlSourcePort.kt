package com.publicplatform.ragops.ingestionops.application.port.out

import com.publicplatform.ragops.ingestionops.domain.CreateCrawlSourceCommand
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary

interface SaveCrawlSourcePort {
    fun createSource(command: CreateCrawlSourceCommand): CrawlSourceSummary
}
