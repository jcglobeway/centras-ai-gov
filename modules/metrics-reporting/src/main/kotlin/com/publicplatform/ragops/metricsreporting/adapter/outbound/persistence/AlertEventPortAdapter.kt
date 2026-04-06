package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import com.publicplatform.ragops.metricsreporting.application.port.out.LoadAlertEventPort
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveAlertEventPort
import com.publicplatform.ragops.metricsreporting.domain.AlertEvent
import java.time.Instant

open class AlertEventPortAdapter(
    private val jpaRepository: JpaAlertEventRepository,
) : LoadAlertEventPort, SaveAlertEventPort {

    override fun findRecent(limit: Int): List<AlertEvent> =
        jpaRepository.findRecent(limit).map { it.toDomain() }

    override fun save(event: AlertEvent) {
        val entity = AlertEventEntity(
            id = event.id,
            metricKey = event.metricKey,
            currentValue = event.currentValue,
            severity = event.severity,
            triggeredAt = Instant.now(),
        )
        jpaRepository.save(entity)
    }
}
