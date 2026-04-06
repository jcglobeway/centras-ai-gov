package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import com.publicplatform.ragops.metricsreporting.domain.AnomalyThreshold
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "alert_thresholds")
class AlertThresholdEntity(
    @Id @Column(name = "metric_key", nullable = false) val metricKey: String,
    @Column(name = "warn_value", nullable = false) val warnValue: BigDecimal,
    @Column(name = "critical_value", nullable = false) val criticalValue: BigDecimal,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant = Instant.now(),
)

fun AlertThresholdEntity.toDomain(): AnomalyThreshold =
    AnomalyThreshold(metricKey = metricKey, warnValue = warnValue, criticalValue = criticalValue, updatedAt = updatedAt)
