package com.publicplatform.ragops.ingestionops.application.port.out

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope

interface LoadIngestionJobPort {
    fun listJobs(scope: IngestionScope): List<IngestionJobSummary>
}
