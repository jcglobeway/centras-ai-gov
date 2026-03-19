package com.publicplatform.ragops.ingestionops.application.port.out

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.RequestIngestionJobCommand
import com.publicplatform.ragops.ingestionops.domain.TransitionIngestionJobCommand

interface PersistIngestionJobPort {
    fun requestJob(command: RequestIngestionJobCommand): IngestionJobSummary
    fun transitionJob(command: TransitionIngestionJobCommand): IngestionJobSummary
}
