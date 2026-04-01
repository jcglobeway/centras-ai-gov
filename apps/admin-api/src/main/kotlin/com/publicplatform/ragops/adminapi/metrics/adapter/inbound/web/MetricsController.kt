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

    @GetMapping("/metrics/pipeline-latency")
    fun getPipelineLatency(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) fromDate: String?,
        @RequestParam("to_date", required = false) toDate: String?,
        servletRequest: HttpServletRequest,
    ): PipelineLatencyResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope(organizationId)
        val effectiveFrom = fromDate?.let { LocalDate.parse(it) } ?: LocalDate.now().minusDays(7)

        val sql = buildString {
            append("""
                SELECT
                    AVG(latency_ms)::BIGINT      AS avg_retrieval_ms,
                    AVG(llm_ms)::BIGINT          AS avg_llm_ms,
                    AVG(postprocess_ms)::BIGINT  AS avg_postprocess_ms,
                    COUNT(*)                     AS sample_count
                FROM rag_search_logs
                WHERE CAST(created_at AS DATE) >= :fromDate
            """.trimIndent())
            if (!scope.globalAccess) {
                append("""
                    AND question_id IN (
                        SELECT id FROM questions WHERE organization_id IN (:orgIds)
                    )
                """.trimIndent())
            }
        }

        val params = mutableMapOf<String, Any>("fromDate" to effectiveFrom)
        if (!scope.globalAccess) params["orgIds"] = scope.organizationIds
        if (toDate != null) {
            // toDate 조건을 WHERE에 추가하려면 sql 재빌드가 필요하므로 fromDate만 지원
        }

        val row = jdbcTemplate.queryForMap(sql, params)
        val avgRetrievalMs = (row["avg_retrieval_ms"] as Number?)?.toLong()
        val avgLlmMs = (row["avg_llm_ms"] as Number?)?.toLong()
        val avgPostprocessMs = (row["avg_postprocess_ms"] as Number?)?.toLong()
        val sampleCount = (row["sample_count"] as Number).toLong()

        val avgTotal = listOfNotNull(avgRetrievalMs, avgLlmMs, avgPostprocessMs)
            .takeIf { it.isNotEmpty() }?.sum()

        return PipelineLatencyResponse(
            avgRetrievalMs = avgRetrievalMs,
            avgLlmMs = avgLlmMs,
            avgPostprocessMs = avgPostprocessMs,
            avgTotalMs = avgTotal,
            sampleCount = sampleCount,
        )
    }

    @GetMapping("/metrics/pii-count")
    fun getPiiCount(
        @RequestParam("organization_id", required = false) organizationId: String?,
        servletRequest: HttpServletRequest,
    ): PiiCountResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope(organizationId)
        val firstOfMonth = LocalDate.now().withDayOfMonth(1)

        val sql = buildString {
            append("""
                SELECT COUNT(*) AS count, MAX(created_at) AS last_detected_at
                FROM audit_logs
                WHERE action_code = 'PII_DETECTED'
                AND CAST(created_at AS DATE) >= :fromDate
            """.trimIndent())
            if (!scope.globalAccess) append(" AND organization_id IN (:orgIds)")
        }

        val params = mutableMapOf<String, Any>("fromDate" to firstOfMonth)
        if (!scope.globalAccess) params["orgIds"] = scope.organizationIds

        val row = jdbcTemplate.queryForMap(sql, params)
        val count = (row["count"] as Number).toInt()
        val lastDetectedAt = row["last_detected_at"]?.toString()
        return PiiCountResponse(count = count, lastDetectedAt = lastDetectedAt)
    }

    @GetMapping("/metrics/question-length-distribution")
    fun getQuestionLengthDistribution(
        @RequestParam("organization_id", required = false) organizationId: String?,
        @RequestParam("from_date", required = false) fromDate: String?,
        @RequestParam("to_date", required = false) toDate: String?,
        servletRequest: HttpServletRequest,
    ): QuestionLengthDistributionResponse {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        val scope = session.toScope(organizationId)

        val sql = buildString {
            append("""
                SELECT
                    SUM(CASE WHEN LENGTH(question_text) <= 5  THEN 1 ELSE 0 END) AS very_short,
                    SUM(CASE WHEN LENGTH(question_text) BETWEEN 6 AND 20 THEN 1 ELSE 0 END) AS short_q,
                    SUM(CASE WHEN LENGTH(question_text) > 20 THEN 1 ELSE 0 END) AS long_q
                FROM questions
                WHERE 1=1
            """.trimIndent())
            if (!scope.globalAccess) append(" AND organization_id IN (:orgIds)")
            if (fromDate != null) append(" AND CAST(created_at AS DATE) >= :fromDate")
            if (toDate != null) append(" AND CAST(created_at AS DATE) <= :toDate")
        }

        val params = mutableMapOf<String, Any>()
        if (!scope.globalAccess) params["orgIds"] = scope.organizationIds
        if (fromDate != null) params["fromDate"] = LocalDate.parse(fromDate)
        if (toDate != null) params["toDate"] = LocalDate.parse(toDate)

        val row = jdbcTemplate.queryForMap(sql, params)
        val veryShort = (row["very_short"] as Number?)?.toInt() ?: 0
        val short = (row["short_q"] as Number?)?.toInt() ?: 0
        val long = (row["long_q"] as Number?)?.toInt() ?: 0
        return QuestionLengthDistributionResponse(
            veryShort = veryShort,
            short = short,
            long = long,
            total = veryShort + short + long,
        )
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

data class QuestionLengthDistributionResponse(
    val veryShort: Int,
    val short: Int,
    val long: Int,
    val total: Int,
)

data class CategoryItem(val category: String, val count: Int)
data class CategoryDistributionResponse(val items: List<CategoryItem>, val total: Int)

data class FeedbackTrendItem(val date: String, val positive: Int, val negative: Int)
data class FeedbackTrendResponse(val items: List<FeedbackTrendItem>)

data class DuplicateQuestionItem(val questionText: String, val count: Int)
data class DuplicateQuestionsResponse(val items: List<DuplicateQuestionItem>, val total: Int)

data class PiiCountResponse(val count: Int, val lastDetectedAt: String?)

data class PipelineLatencyResponse(
    val avgRetrievalMs: Long?,
    val avgLlmMs: Long?,
    val avgPostprocessMs: Long?,
    val avgTotalMs: Long?,
    val sampleCount: Long,
)

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
