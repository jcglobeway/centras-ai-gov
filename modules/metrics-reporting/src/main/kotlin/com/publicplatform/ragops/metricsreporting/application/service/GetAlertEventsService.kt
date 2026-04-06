package com.publicplatform.ragops.metricsreporting.application.service

import com.publicplatform.ragops.metricsreporting.application.port.`in`.GetAlertEventsUseCase
import com.publicplatform.ragops.metricsreporting.application.port.out.LoadAlertEventPort
import com.publicplatform.ragops.metricsreporting.domain.AlertEvent

class GetAlertEventsService(
    private val loadAlertEventPort: LoadAlertEventPort,
) : GetAlertEventsUseCase {
    override fun getRecent(limit: Int): List<AlertEvent> = loadAlertEventPort.findRecent(limit)
}
