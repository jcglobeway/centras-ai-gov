package com.publicplatform.ragops.adminapi.metrics.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.adminapi.metrics.adapter.inbound.scheduler.MetricsAggregationScheduler
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.metricsreporting.application.port.`in`.ListMetricsUseCase
import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.MetricsScope
import jakarta.servlet.http.HttpServletRequest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 지표 대시보드 HTTP 인바운드 어댑터.
 *
 * 일별 KPI 지표 목록 조회를 ListMetricsUseCase에 위임한다.
 */
@RestController
@RequestMapping("/admin")
class MetricsController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val listMetricsUseCase: ListMetricsUseCase,
    private val metricsAggregationScheduler: MetricsAggregationScheduler,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {

    @GetMapping("/metrics/daily")
    fun listDailyMetrics(
        @RequestParam("from_date", required = false) fromDate: String?,
        @RequestParam("to_date", required = false) toDate: String?,
        @RequestParam("organization_id", required = false) organizationId: String?,
        servletRequest: HttpServletRequest,
    ): DailyMetricsListResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)

        val metrics = listMetricsUseCase.execute(
            scope = session.toScope(organizationId),
            fromDate = fromDate?.let { LocalDate.parse(it) },
            toDate = toDate?.let { LocalDate.parse(it) },
        )

        return DailyMetricsListResponse(items = metrics.map { it.toResponse() }, total = metrics.size)
    }

    @GetMapping("/metrics/category-distribution")
    fun getCategoryDistribution(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) fromDate: String?,
        @RequestParam("to_date", required = false) toDate: String?,
        servletRequest: HttpServletRequest,
    ): CategoryDistributionResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope(organizationId)

        val sql = buildString {
            append("""
                SELECT COALESCE(question_category, '미분류') AS category, COUNT(*) AS count
                FROM questions
                WHERE 1=1
            """.trimIndent())
            if (!scope.globalAccess) append(" AND organization_id IN (:orgIds)")
            if (fromDate != null) append(" AND CAST(created_at AS DATE) >= :fromDate")
            if (toDate != null) append(" AND CAST(created_at AS DATE) <= :toDate")
            append(" GROUP BY COALESCE(question_category, '미분류') ORDER BY count DESC")
        }

        val params = mutableMapOf<String, Any>()
        if (!scope.globalAccess) params["orgIds"] = scope.organizationIds
        if (fromDate != null) params["fromDate"] = LocalDate.parse(fromDate)
        if (toDate != null) params["toDate"] = LocalDate.parse(toDate)

        val rows = jdbcTemplate.queryForList(sql, params)
        val items = rows.map { row ->
            CategoryItem(
                category = row["category"] as String,
                count = (row["count"] as Number).toInt(),
            )
        }
        val total = items.sumOf { it.count }
        return CategoryDistributionResponse(items = items, total = total)
    }

    @GetMapping("/metrics/feedback-trend")
    fun getFeedbackTrend(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("days", defaultValue = "7") days: Int,
        servletRequest: HttpServletRequest,
    ): FeedbackTrendResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope(organizationId)
        val fromDate = LocalDate.now().minusDays(days.toLong())

        val sql = buildString {
            append("""
                SELECT CAST(submitted_at AS DATE) AS day,
                       SUM(CASE WHEN rating >= 4 THEN 1 ELSE 0 END) AS positive,
                       SUM(CASE WHEN rating <= 2 THEN 1 ELSE 0 END) AS negative
                FROM feedbacks
                WHERE CAST(submitted_at AS DATE) >= :fromDate
            """.trimIndent())
            if (!scope.globalAccess) append(" AND organization_id IN (:orgIds)")
            append(" GROUP BY CAST(submitted_at AS DATE) ORDER BY day ASC")
        }

        val params = mutableMapOf<String, Any>("fromDate" to fromDate)
        if (!scope.globalAccess) params["orgIds"] = scope.organizationIds

        val rows = jdbcTemplate.queryForList(sql, params)
        val items = rows.map { row ->
            FeedbackTrendItem(
                date = row["day"].toString(),
                positive = (row["positive"] as Number).toInt(),
                negative = (row["negative"] as Number).toInt(),
            )
        }
        return FeedbackTrendResponse(items = items)
    }

    @GetMapping("/metrics/duplicate-questions")
    fun getDuplicateQuestions(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) fromDate: String?,
        @RequestParam("to_date", required = false) toDate: String?,
        @RequestParam("min_count", defaultValue = "2") minCount: Int,
        @RequestParam("limit", defaultValue = "10") limit: Int,
        servletRequest: HttpServletRequest,
    ): DuplicateQuestionsResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope(organizationId)

        val sql = buildString {
            append("""
                SELECT question_text, COUNT(*) AS count
                FROM questions
                WHERE 1=1
            """.trimIndent())
            if (!scope.globalAccess) append(" AND organization_id IN (:orgIds)")
            if (fromDate != null) append(" AND CAST(created_at AS DATE) >= :fromDate")
            if (toDate != null) append(" AND CAST(created_at AS DATE) <= :toDate")
            append(" GROUP BY question_text HAVING COUNT(*) >= :minCount ORDER BY count DESC LIMIT :limit")
        }

        val params = mutableMapOf<String, Any>("minCount" to minCount, "limit" to limit)
        if (!scope.globalAccess) params["orgIds"] = scope.organizationIds
        if (fromDate != null) params["fromDate"] = LocalDate.parse(fromDate)
        if (toDate != null) params["toDate"] = LocalDate.parse(toDate)

        val rows = jdbcTemplate.queryForList(sql, params)
        val items = rows.map { row ->
            DuplicateQuestionItem(
                questionText = row["question_text"] as String,
                count = (row["count"] as Number).toInt(),
            )
        }
        return DuplicateQuestionsResponse(items = items, total = items.sumOf { it.count })
    }

    @PostMapping("/metrics/aggregate")
    fun triggerAggregation(
        @RequestParam("date", required = false) date: String?,
        servletRequest: HttpServletRequest,
    ): AggregationTriggeredResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val targetDate = date?.let { LocalDate.parse(it) } ?: LocalDate.now().minusDays(1)
        metricsAggregationScheduler.aggregate(targetDate)
        return AggregationTriggeredResponse(targetDate = targetDate.toString())
    }
}

data class AggregationTriggeredResponse(val targetDate: String)

data class CategoryItem(val category: String, val count: Int)
data class CategoryDistributionResponse(val items: List<CategoryItem>, val total: Int)

data class FeedbackTrendItem(val date: String, val positive: Int, val negative: Int)
data class FeedbackTrendResponse(val items: List<FeedbackTrendItem>)

data class DuplicateQuestionItem(val questionText: String, val count: Int)
data class DuplicateQuestionsResponse(val items: List<DuplicateQuestionItem>, val total: Int)

data class DailyMetricsListResponse(val items: List<DailyMetricsResponse>, val total: Int)

data class DailyMetricsResponse(
    val id: String, val metricDate: String, val organizationId: String, val serviceId: String,
    val totalSessions: Int, val totalQuestions: Int, val resolvedRate: BigDecimal?,
    val fallbackRate: BigDecimal?, val zeroResultRate: BigDecimal?, val avgResponseTimeMs: Int?,
    val autoResolutionRate: BigDecimal?, val escalationRate: BigDecimal?,
    val revisitRate: BigDecimal?, val afterHoursRate: BigDecimal?,
    val knowledgeGapCount: Int, val unansweredCount: Int, val lowSatisfactionCount: Int,
)

private fun DailyMetricsSummary.toResponse() = DailyMetricsResponse(
    id = id, metricDate = metricDate.toString(), organizationId = organizationId,
    serviceId = serviceId, totalSessions = totalSessions, totalQuestions = totalQuestions,
    resolvedRate = resolvedRate, fallbackRate = fallbackRate, zeroResultRate = zeroResultRate,
    avgResponseTimeMs = avgResponseTimeMs,
    autoResolutionRate = autoResolutionRate, escalationRate = escalationRate,
    revisitRate = revisitRate, afterHoursRate = afterHoursRate,
    knowledgeGapCount = knowledgeGapCount, unansweredCount = unansweredCount,
    lowSatisfactionCount = lowSatisfactionCount,
)

private fun AdminSessionSnapshot.toScope(filterOrgId: String? = null): MetricsScope {
    val globalAccess = roleAssignments.any { it.organizationId == null }
    val sessionOrgIds = roleAssignments.mapNotNull { it.organizationId }.toSet()
    return if (filterOrgId != null) {
        val allowed = globalAccess || filterOrgId in sessionOrgIds
        MetricsScope(organizationIds = if (allowed) setOf(filterOrgId) else sessionOrgIds, globalAccess = false)
    } else {
        MetricsScope(organizationIds = sessionOrgIds, globalAccess = globalAccess)
    }
}
