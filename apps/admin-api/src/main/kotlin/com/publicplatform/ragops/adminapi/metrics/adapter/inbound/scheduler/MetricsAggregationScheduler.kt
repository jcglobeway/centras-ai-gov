/**
 * 일별 KPI 지표 자동 집계 스케줄러.
 *
 * `metrics.aggregation.cron` 설정(기본값: 30분마다)에 따라 직전 30분 단위 시각의
 * 질문·답변 데이터를 집계하여 daily_metrics_org에 upsert한다.
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

    // cron 값은 application.yml의 metrics.aggregation.cron으로 외부화되어 있다.
    // 기본값("0 5 0 * * *")은 설정 누락 시 폴백용으로만 유지한다.
    @Scheduled(cron = "\${metrics.aggregation.cron:0 5 0 * * *}")
    fun aggregatePreviousDay() {
        aggregate(LocalDate.now().minusDays(1))
    }

    fun aggregate(targetDate: LocalDate) {
        val mainSql = """
            SELECT
                q.organization_id,
                q.service_id,
                COUNT(DISTINCT q.chat_session_id)                                                     AS total_sessions,
                COUNT(q.id)                                                                            AS total_questions,
                SUM(CASE WHEN a.answer_status = 'fallback'  THEN 1 ELSE 0 END)                        AS fallback_count,
                SUM(CASE WHEN a.answer_status = 'no_answer' THEN 1 ELSE 0 END)                        AS no_answer_count,
                AVG(CAST(a.response_time_ms AS DOUBLE PRECISION))                                     AS avg_response_time_ms,
                SUM(CASE WHEN a.answer_status = 'answered' AND q.is_escalated = false THEN 1 ELSE 0 END) AS auto_resolved_count,
                SUM(CASE WHEN q.is_escalated = true THEN 1 ELSE 0 END)                                AS escalated_count,
                SUM(CASE WHEN a.answer_status = 'answered' THEN 1 ELSE 0 END)                         AS answered_count,
                SUM(CASE WHEN q.failure_reason_code = 'A01' THEN 1 ELSE 0 END)                        AS knowledge_gap_count,
                SUM(CASE WHEN a.answer_status IN ('fallback', 'no_answer') OR q.is_escalated = true THEN 1 ELSE 0 END) AS unanswered_count,
                SUM(CASE WHEN EXTRACT(HOUR FROM q.created_at) < 9 OR EXTRACT(HOUR FROM q.created_at) >= 18 THEN 1 ELSE 0 END) AS after_hours_count,
                AVG(sess.turn_count)                                                                   AS avg_session_turn_count
            FROM questions q
            LEFT JOIN answers a ON q.id = a.question_id
            LEFT JOIN (
                SELECT chat_session_id, COUNT(*) AS turn_count
                FROM questions
                WHERE CAST(created_at AS DATE) = :targetDate
                GROUP BY chat_session_id
            ) sess ON q.chat_session_id = sess.chat_session_id
            WHERE CAST(q.created_at AS DATE) = :targetDate
            GROUP BY q.organization_id, q.service_id
        """.trimIndent()

        val feedbackSql = """
            SELECT
                organization_id,
                service_id,
                SUM(CASE WHEN target_action_completed = true THEN 1 ELSE 0 END) AS explicit_resolved_count,
                COUNT(id)                                                         AS feedback_count,
                SUM(CASE WHEN rating <= 2 THEN 1 ELSE 0 END)                    AS low_satisfaction_count,
                COUNT(DISTINCT session_id)                                        AS revisit_sessions
            FROM feedbacks
            WHERE CAST(submitted_at AS DATE) = :targetDate
            GROUP BY organization_id, service_id
        """.trimIndent()

        // 활성 기관-서비스 목록 (질문 없는 날도 0-행 생성하기 위해)
        val activeServicesSql = """
            SELECT s.organization_id, s.id AS service_id
            FROM services s
            JOIN organizations o ON s.organization_id = o.id
            WHERE o.status = 'active'
        """.trimIndent()
        val activeServices = jdbcTemplate.queryForList(activeServicesSql, emptyMap<String, Any>())
            .map { "${it["organization_id"]}_${it["service_id"]}" to it }
            .toMap()

        val params = mapOf("targetDate" to targetDate)
        val mainRows = jdbcTemplate.queryForList(mainSql, params)
        val feedbackRows = jdbcTemplate.queryForList(feedbackSql, params)
            .associateBy { "${it["organization_id"]}_${it["service_id"]}" }

        // 실데이터 집계 결과에 없는 활성 서비스는 0-행으로 채움
        val coveredKeys = mainRows.map { "${it["organization_id"]}_${it["service_id"]}" }.toSet()
        val zeroRows = activeServices.filterKeys { it !in coveredKeys }.values.map { svc ->
            mapOf(
                "organization_id" to svc["organization_id"],
                "service_id" to svc["service_id"],
                "total_sessions" to 0L, "total_questions" to 0L,
                "fallback_count" to 0L, "no_answer_count" to 0L,
                "avg_response_time_ms" to null,
                "auto_resolved_count" to 0L, "escalated_count" to 0L,
                "answered_count" to 0L, "knowledge_gap_count" to 0L,
                "unanswered_count" to 0L, "after_hours_count" to 0L,
                "avg_session_turn_count" to null,
            )
        }

        for (row in mainRows + zeroRows) {
            val totalQuestions = (row["total_questions"] as Number).toLong()

            val totalSessions = (row["total_sessions"] as Number).toLong()
            val fallbackCount  = (row["fallback_count"]    as Number? ?: 0).toLong()
            val noAnswerCount  = (row["no_answer_count"]   as Number? ?: 0).toLong()
            val autoResolved   = (row["auto_resolved_count"] as Number? ?: 0).toLong()
            val escalated      = (row["escalated_count"]   as Number? ?: 0).toLong()
            val answered       = (row["answered_count"]    as Number? ?: 0).toLong()
            val afterHours     = (row["after_hours_count"] as Number? ?: 0).toLong()
            val avgMs          = (row["avg_response_time_ms"] as Number?)?.toDouble()
            val avgTurnCount   = (row["avg_session_turn_count"] as Number?)?.toDouble()

            val scale4 = java.math.RoundingMode.HALF_UP
            fun rate4(n: Long, d: Long) = if (d == 0L) BigDecimal.ZERO.setScale(4, scale4) else BigDecimal(n.toDouble() / d).setScale(4, scale4)
            fun rate2(n: Long, d: Long) = if (d == 0L) BigDecimal.ZERO.setScale(2, scale4) else BigDecimal(n * 100.0 / d).setScale(2, scale4)

            val orgService = "${row["organization_id"]}_${row["service_id"]}"
            val fb = feedbackRows[orgService]
            val feedbackCount     = (fb?.get("feedback_count")          as Number? ?: 0).toLong()
            val explicitResolved  = (fb?.get("explicit_resolved_count") as Number? ?: 0).toLong()
            val lowSatisfaction   = (fb?.get("low_satisfaction_count")  as Number? ?: 0).toInt()
            val revisitSessions   = (fb?.get("revisit_sessions")        as Number? ?: 0).toLong()

            metricsWriter.upsertDailyMetrics(
                SaveDailyMetricsCommand(
                    metricDate        = targetDate,
                    organizationId    = row["organization_id"] as String,
                    serviceId         = row["service_id"] as String,
                    totalSessions     = totalSessions.toInt(),
                    totalQuestions    = totalQuestions.toInt(),
                    resolvedRate      = rate2(answered, totalQuestions),
                    fallbackRate      = rate2(fallbackCount, totalQuestions),
                    zeroResultRate    = rate2(noAnswerCount, totalQuestions),
                    avgResponseTimeMs = avgMs?.toInt(),
                    autoResolutionRate      = rate4(autoResolved, totalQuestions),
                    escalationRate          = rate4(escalated, totalQuestions),
                    explicitResolutionRate  = if (feedbackCount > 0) rate4(explicitResolved, feedbackCount) else null,
                    estimatedResolutionRate = rate4(answered, totalQuestions),
                    revisitRate             = if (totalSessions > 0) rate4(revisitSessions, totalSessions) else null,
                    afterHoursRate          = rate4(afterHours, totalQuestions),
                    avgSessionTurnCount     = avgTurnCount?.let { BigDecimal(it).setScale(2, scale4) },
                    knowledgeGapCount       = (row["knowledge_gap_count"] as Number? ?: 0).toInt(),
                    unansweredCount         = (row["unanswered_count"]    as Number? ?: 0).toInt(),
                    lowSatisfactionCount    = lowSatisfaction,
                ),
            )
        }
    }
}
