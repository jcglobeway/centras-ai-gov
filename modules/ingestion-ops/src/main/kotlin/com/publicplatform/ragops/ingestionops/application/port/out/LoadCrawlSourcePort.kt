package com.publicplatform.ragops.ingestionops.application.port.out

import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope

interface LoadCrawlSourcePort {
    fun listSources(scope: IngestionScope): List<CrawlSourceSummary>
}
