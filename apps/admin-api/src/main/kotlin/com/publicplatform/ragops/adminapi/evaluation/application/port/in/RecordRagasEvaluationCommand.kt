package com.publicplatform.ragops.adminapi.evaluation.application.port.`in`

/**
 * eval-runner가 RAGAS 평가 결과를 저장할 때 사용하는 커맨드.
 *
 * organizationId는 realtime-eval이 Redis 큐에서 꺼낸 페이로드에서 전달하며,
 * 레거시 수동 호출 시에는 null이 허용된다.
 */
data class RecordRagasEvaluationCommand(
    val questionId: String,
    val organizationId: String? = null,
    val faithfulness: Double?,
    val answerRelevancy: Double?,
    val contextPrecision: Double?,
    val contextRecall: Double?,
    val citationCoverage: Double?,
    val citationCorrectness: Double?,
    val judgeProvider: String?,
    val judgeModel: String?,
)
