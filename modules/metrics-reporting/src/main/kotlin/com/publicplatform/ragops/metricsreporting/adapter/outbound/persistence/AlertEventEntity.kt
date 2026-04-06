package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import com.publicplatform.ragops.metricsreporting.domain.AlertEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "alert_events")
class AlertEventEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "metric_key", nullable = false) val metricKey: String,
    @Column(name = "current_value", nullable = false) val currentValue: BigDecimal,
    @Column(name = "severity", nullable = false) val severity: String,
    @Column(name = "triggered_at", nullable = false) val triggeredAt: Instant = Instant.now(),
)

fun AlertEventEntity.toDomain(): AlertEvent =
    AlertEvent(id = id, metricKey = metricKey, currentValue = currentValue, severity = severity, triggeredAt = triggeredAt)
