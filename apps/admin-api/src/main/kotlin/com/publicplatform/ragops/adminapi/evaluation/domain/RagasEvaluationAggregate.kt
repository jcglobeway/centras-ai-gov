package com.publicplatform.ragops.adminapi.evaluation.domain

import java.time.LocalDate

/**
 * 특정 기간의 RAGAS 지표 집계 결과.
 *
 * 질문별 개별 평가 레코드(ragas_evaluations)를 AVG 집계한 값이다.
 * 평가 데이터가 없으면 avg 필드는 null, count = 0으로 반환된다.
 */
data class RagasEvaluationPeriodSummary(
    val avgFaithfulness: Double?,
    val avgAnswerRelevancy: Double?,
    val avgContextPrecision: Double?,
    val avgContextRecall: Double?,
    val avgCitationCoverage: Double?,
    val avgCitationCorrectness: Double?,
    val count: Long,
    val from: LocalDate,
    val to: LocalDate,
)

/**
 * 현재 기간과 직전 동일 길이 기간의 집계를 묶은 집계 결과.
 *
 * 대시보드의 Δ(delta) 스코어카드에서 current - previous 로 변화량을 계산한다.
 */
data class RagasEvaluationAggregate(
    val current: RagasEvaluationPeriodSummary,
    val previous: RagasEvaluationPeriodSummary,
)
