package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.SaveDailyMetricsCommand
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveMetricsPort
import java.time.Instant
import java.util.UUID

open class SaveMetricsPortAdapter(
    private val jpaRepository: JpaDailyMetricsRepository,
) : SaveMetricsPort {

    override fun upsertDailyMetrics(command: SaveDailyMetricsCommand): DailyMetricsSummary {
        val existing = jpaRepository.findByMetricDateAndOrganizationIdAndServiceId(
            command.metricDate, command.organizationId, command.serviceId,
        )

        val entity = if (existing != null) {
            DailyMetricsEntity(
                id = existing.id, metricDate = command.metricDate,
                organizationId = command.organizationId, serviceId = command.serviceId,
                totalSessions = command.totalSessions, totalQuestions = command.totalQuestions,
                resolvedRate = null, fallbackRate = command.fallbackRate,
                zeroResultRate = command.zeroResultRate, avgResponseTimeMs = command.avgResponseTimeMs,
                createdAt = existing.createdAt,
            )
        } else {
            DailyMetricsEntity(
                id = "metric_${UUID.randomUUID().toString().substring(0, 8)}",
                metricDate = command.metricDate, organizationId = command.organizationId,
                serviceId = command.serviceId, totalSessions = command.totalSessions,
                totalQuestions = command.totalQuestions, resolvedRate = null,
                fallbackRate = command.fallbackRate, zeroResultRate = command.zeroResultRate,
                avgResponseTimeMs = command.avgResponseTimeMs, createdAt = Instant.now(),
            )
        }

        return jpaRepository.save(entity).toSummary()
    }
}
