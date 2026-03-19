/**
 * 일별 KPI 지표 저장 아웃바운드 포트.
 *
 * upsert 방식으로 동작하므로 같은 날짜·기관·서비스 조합은 기존 레코드를 덮어쓴다.
 * 스케줄러 재실행 시 멱등성을 보장하기 위해 ON CONFLICT DO UPDATE를 사용한다.
 */
package com.publicplatform.ragops.metricsreporting.application.port.out

import com.publicplatform.ragops.metricsreporting.domain.DailyMetricsSummary
import com.publicplatform.ragops.metricsreporting.domain.SaveDailyMetricsCommand

interface SaveMetricsPort {
    fun upsertDailyMetrics(command: SaveDailyMetricsCommand): DailyMetricsSummary
}
