/**
 * 일별 KPI 지표 자동 집계 스케줄러.
 *
 * 매일 00:05에 전날의 질문·답변 데이터를 집계하여 daily_metrics_org에 upsert한다.
 * 크로스 모듈 집계를 위해 NamedParameterJdbcTemplate을 사용하며,
 * 모듈 간 순환 의존을 피하고자 JPA 대신 Native SQL로 처리한다.
 */
package com.publicplatform.ragops.adminapi.metrics.adapter.inbound.scheduler

import com.publicplatform.ragops.metricsreporting.domain.SaveDailyMetricsCommand
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveMetricsPort
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class MetricsAggregationScheduler(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val metricsWriter: SaveMetricsPort,
) {

    // 매일 00:05에 전날 집계 실행
    @Scheduled(cron = "0 5 0 * * *")
    fun aggregatePreviousDay() {
        aggregate(LocalDate.now().minusDays(1))
    }

    fun aggregate(targetDate: LocalDate) {
        val sql = """
            SELECT
                q.organization_id,
                q.service_id,
                COUNT(DISTINCT q.chat_session_id) AS total_sessions,
                COUNT(q.id)                        AS total_questions,
                SUM(CASE WHEN a.answer_status = 'fallback'  THEN 1 ELSE 0 END)  AS fallback_count,
                SUM(CASE WHEN a.answer_status = 'no_answer' THEN 1 ELSE 0 END)  AS no_answer_count,
                AVG(CAST(a.response_time_ms AS DOUBLE PRECISION))               AS avg_response_time_ms
            FROM questions q
            LEFT JOIN answers a ON q.id = a.question_id
            WHERE CAST(q.created_at AS DATE) = :targetDate
            GROUP BY q.organization_id, q.service_id
        """.trimIndent()

        val rows = jdbcTemplate.queryForList(sql, mapOf("targetDate" to targetDate))

        for (row in rows) {
            val totalQuestions = (row["total_questions"] as Number).toLong()
            if (totalQuestions == 0L) continue

            val fallbackCount = (row["fallback_count"] as Number? ?: 0).toLong()
            val noAnswerCount = (row["no_answer_count"] as Number? ?: 0).toLong()
            val avgMs = (row["avg_response_time_ms"] as Number?)?.toDouble()

            val fallbackRate = BigDecimal(fallbackCount * 100.0 / totalQuestions)
                .setScale(2, java.math.RoundingMode.HALF_UP)
            val zeroResultRate = BigDecimal(noAnswerCount * 100.0 / totalQuestions)
                .setScale(2, java.math.RoundingMode.HALF_UP)

            metricsWriter.upsertDailyMetrics(
                SaveDailyMetricsCommand(
                    metricDate = targetDate,
                    organizationId = row["organization_id"] as String,
                    serviceId = row["service_id"] as String,
                    totalSessions = (row["total_sessions"] as Number).toInt(),
                    totalQuestions = totalQuestions.toInt(),
                    fallbackRate = fallbackRate,
                    zeroResultRate = zeroResultRate,
                    avgResponseTimeMs = avgMs?.toInt(),
                ),
            )
        }
    }
}
