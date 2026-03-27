/**
 * SaveMetricsPort의 JPA 구현체.
 *
 * 동일 날짜·기관·서비스 조합의 지표를 upsert 방식으로 저장한다.
 * MetricsAggregationScheduler가 매일 00:05에 호출한다.
 */
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
                resolvedRate = command.resolvedRate, fallbackRate = command.fallbackRate,
                zeroResultRate = command.zeroResultRate, avgResponseTimeMs = command.avgResponseTimeMs,
                autoResolutionRate = command.autoResolutionRate,
                escalationRate = command.escalationRate,
                explicitResolutionRate = command.explicitResolutionRate,
                estimatedResolutionRate = command.estimatedResolutionRate,
                revisitRate = command.revisitRate,
                afterHoursRate = command.afterHoursRate,
                avgSessionTurnCount = command.avgSessionTurnCount,
                knowledgeGapCount = command.knowledgeGapCount,
                unansweredCount = command.unansweredCount,
                lowSatisfactionCount = command.lowSatisfactionCount,
                createdAt = existing.createdAt,
            )
        } else {
            DailyMetricsEntity(
                id = "metric_${UUID.randomUUID().toString().substring(0, 8)}",
                metricDate = command.metricDate, organizationId = command.organizationId,
                serviceId = command.serviceId, totalSessions = command.totalSessions,
                totalQuestions = command.totalQuestions, resolvedRate = null,
                fallbackRate = command.fallbackRate, zeroResultRate = command.zeroResultRate,
                avgResponseTimeMs = command.avgResponseTimeMs,
                autoResolutionRate = command.autoResolutionRate,
                escalationRate = command.escalationRate,
                explicitResolutionRate = command.explicitResolutionRate,
                estimatedResolutionRate = command.estimatedResolutionRate,
                revisitRate = command.revisitRate,
                afterHoursRate = command.afterHoursRate,
                avgSessionTurnCount = command.avgSessionTurnCount,
                knowledgeGapCount = command.knowledgeGapCount,
                unansweredCount = command.unansweredCount,
                lowSatisfactionCount = command.lowSatisfactionCount,
                createdAt = Instant.now(),
            )
        }

        return jpaRepository.save(entity).toSummary()
    }
}
