/**
 * 지정 날짜의 KPI 집계를 즉시 실행하는 인바운드 포트.
 *
 * 스케줄 주기(30분)를 기다리지 않고 특정 날짜의 지표를 온디맨드로 집계할 때 사용한다.
 * 주로 데이터 투입 직후 대시보드를 즉시 확인해야 하는 상황에서 호출된다.
 */
package com.publicplatform.ragops.adminapi.metrics.application.port.`in`

import java.time.LocalDate

interface TriggerMetricsAggregationUseCase {
    fun trigger(date: LocalDate)
}
