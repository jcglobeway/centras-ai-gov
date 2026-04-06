package com.publicplatform.ragops.metricsreporting.application.port.`in`

import com.publicplatform.ragops.metricsreporting.domain.AlertEvent
import com.publicplatform.ragops.metricsreporting.domain.AnomalyThreshold
import com.publicplatform.ragops.metricsreporting.domain.DriftSummary
import com.publicplatform.ragops.metricsreporting.domain.UpdateThresholdCommand

interface GetAnomalyThresholdsUseCase {
    fun getAll(): List<AnomalyThreshold>
}

interface UpdateAnomalyThresholdsUseCase {
    fun update(commands: List<UpdateThresholdCommand>): List<AnomalyThreshold>
}

interface GetAlertEventsUseCase {
    fun getRecent(limit: Int): List<AlertEvent>
}

interface GetDriftSummaryUseCase {
    fun getSummary(organizationIds: Set<String>, globalAccess: Boolean): List<DriftSummary>
}
