/**
 * DailyMetrics DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "daily_metrics_org")
class DailyMetricsEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "metric_date", nullable = false) val metricDate: LocalDate,
    @Column(name = "organization_id", nullable = false) val organizationId: String,
    @Column(name = "service_id", nullable = false) val serviceId: String,
    @Column(name = "total_sessions", nullable = false) val totalSessions: Int = 0,
    @Column(name = "total_questions", nullable = false) val totalQuestions: Int = 0,
    @Column(name = "resolved_rate", precision = 5, scale = 2) val resolvedRate: BigDecimal?,
    @Column(name = "fallback_rate", precision = 5, scale = 2) val fallbackRate: BigDecimal?,
    @Column(name = "zero_result_rate", precision = 5, scale = 2) val zeroResultRate: BigDecimal?,
    @Column(name = "avg_response_time_ms") val avgResponseTimeMs: Int?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)

fun DailyMetricsEntity.toSummary(): DailyMetricsSummary =
    DailyMetricsSummary(
        id = id, metricDate = metricDate, organizationId = organizationId, serviceId = serviceId,
        totalSessions = totalSessions, totalQuestions = totalQuestions,
        resolvedRate = resolvedRate, fallbackRate = fallbackRate,
        zeroResultRate = zeroResultRate, avgResponseTimeMs = avgResponseTimeMs, createdAt = createdAt,
    )
