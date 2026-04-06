package com.publicplatform.ragops.adminapi.metrics.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.metricsreporting.application.port.`in`.GetAlertEventsUseCase
import com.publicplatform.ragops.metricsreporting.application.port.`in`.GetAnomalyThresholdsUseCase
import com.publicplatform.ragops.metricsreporting.application.port.`in`.GetDriftSummaryUseCase
import com.publicplatform.ragops.metricsreporting.application.port.`in`.UpdateAnomalyThresholdsUseCase
import com.publicplatform.ragops.metricsreporting.domain.AlertEvent
import com.publicplatform.ragops.metricsreporting.domain.AnomalyThreshold
import com.publicplatform.ragops.metricsreporting.domain.DriftSummary
import com.publicplatform.ragops.metricsreporting.domain.UpdateThresholdCommand
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant

@RestController
@RequestMapping("/admin/anomaly")
class AnomalyController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val getAnomalyThresholdsUseCase: GetAnomalyThresholdsUseCase,
    private val updateAnomalyThresholdsUseCase: UpdateAnomalyThresholdsUseCase,
    private val getAlertEventsUseCase: GetAlertEventsUseCase,
    private val getDriftSummaryUseCase: GetDriftSummaryUseCase,
) {

    @GetMapping("/thresholds")
    fun getThresholds(servletRequest: HttpServletRequest): ThresholdsResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val thresholds = getAnomalyThresholdsUseCase.getAll()
        return ThresholdsResponse(items = thresholds.map { it.toResponse() })
    }

    @PutMapping("/thresholds")
    fun updateThresholds(
        @RequestBody request: UpdateThresholdsRequest,
        servletRequest: HttpServletRequest,
    ): ThresholdsResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val commands = request.thresholds.map {
            UpdateThresholdCommand(
                metricKey = it.metricKey,
                warnValue = it.warnValue,
                criticalValue = it.criticalValue,
            )
        }
        val updated = updateAnomalyThresholdsUseCase.update(commands)
        return ThresholdsResponse(items = updated.map { it.toResponse() })
    }

    @GetMapping("/alert-events")
    fun getAlertEvents(
        @RequestParam("limit", defaultValue = "50") limit: Int,
        servletRequest: HttpServletRequest,
    ): AlertEventsResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val events = getAlertEventsUseCase.getRecent(limit)
        return AlertEventsResponse(items = events.map { it.toResponse() }, total = events.size)
    }

    @GetMapping("/drift-summary")
    fun getDriftSummary(
        @RequestParam("organization_id", required = false) organizationId: String?,
        servletRequest: HttpServletRequest,
    ): DriftSummaryResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val globalAccess = session.roleAssignments.any { it.organizationId == null }
        val orgIds = if (organizationId != null) setOf(organizationId)
                     else session.roleAssignments.mapNotNull { it.organizationId }.toSet()
        val summaries = getDriftSummaryUseCase.getSummary(orgIds, globalAccess)
        return DriftSummaryResponse(items = summaries.map { it.toResponse() })
    }
}

data class ThresholdResponse(
    val metricKey: String,
    val warnValue: BigDecimal,
    val criticalValue: BigDecimal,
    val updatedAt: String,
)

data class ThresholdsResponse(val items: List<ThresholdResponse>)

data class ThresholdUpdateItem(val metricKey: String, val warnValue: BigDecimal, val criticalValue: BigDecimal)
data class UpdateThresholdsRequest(val thresholds: List<ThresholdUpdateItem>)

data class AlertEventResponse(
    val id: String,
    val metricKey: String,
    val currentValue: BigDecimal,
    val severity: String,
    val triggeredAt: String,
)

data class AlertEventsResponse(val items: List<AlertEventResponse>, val total: Int)

data class DriftSummaryItem(
    val metricKey: String,
    val rollingAvg: BigDecimal?,
    val latestValue: BigDecimal?,
    val deviationPct: BigDecimal?,
)

data class DriftSummaryResponse(val items: List<DriftSummaryItem>)

private fun AnomalyThreshold.toResponse() = ThresholdResponse(
    metricKey = metricKey,
    warnValue = warnValue,
    criticalValue = criticalValue,
    updatedAt = updatedAt.toString(),
)

private fun AlertEvent.toResponse() = AlertEventResponse(
    id = id,
    metricKey = metricKey,
    currentValue = currentValue,
    severity = severity,
    triggeredAt = triggeredAt.toString(),
)

private fun DriftSummary.toResponse() = DriftSummaryItem(
    metricKey = metricKey,
    rollingAvg = rollingAvg,
    latestValue = latestValue,
    deviationPct = deviationPct,
)
