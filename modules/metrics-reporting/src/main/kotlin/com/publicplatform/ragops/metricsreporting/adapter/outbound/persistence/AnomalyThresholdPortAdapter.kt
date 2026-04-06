package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import com.publicplatform.ragops.metricsreporting.application.port.out.LoadAnomalyThresholdPort
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveAnomalyThresholdPort
import com.publicplatform.ragops.metricsreporting.domain.AnomalyThreshold
import java.time.Instant

open class AnomalyThresholdPortAdapter(
    private val jpaRepository: JpaAlertThresholdRepository,
) : LoadAnomalyThresholdPort, SaveAnomalyThresholdPort {

    override fun findAll(): List<AnomalyThreshold> =
        jpaRepository.findAll().map { it.toDomain() }

    override fun findByKey(metricKey: String): AnomalyThreshold? =
        jpaRepository.findById(metricKey).orElse(null)?.toDomain()

    override fun save(threshold: AnomalyThreshold): AnomalyThreshold {
        val entity = AlertThresholdEntity(
            metricKey = threshold.metricKey,
            warnValue = threshold.warnValue,
            criticalValue = threshold.criticalValue,
            updatedAt = Instant.now(),
        )
        return jpaRepository.save(entity).toDomain()
    }
}
