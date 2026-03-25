/**
 * 시민이 제출한 질문과 그 상태의 요약 뷰.
 *
 * answerConfidence는 RAG 파이프라인이 반환한 신뢰도 점수이며,
 * failureReasonCode는 미해결 원인 표준 코드(A01~A10)를 담는다.
 * CreateQuestionCommand는 새 질문 생성 시 사용하는 입력 커맨드이다.
 */
package com.publicplatform.ragops.chatruntime.domain

import java.math.BigDecimal
import java.time.Instant

data class QuestionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val questionCategory: String?,
    val answerConfidence: BigDecimal?,
    val failureReasonCode: FailureReasonCode?,
    val isEscalated: Boolean,
    val createdAt: Instant,
)

data class CreateQuestionCommand(
    val organizationId: String,
    val serviceId: String,
    val chatSessionId: String,
    val questionText: String,
    val questionIntentLabel: String?,
    val channel: String,
    val questionCategory: String? = null,
    val failureReasonCode: FailureReasonCode? = null,
)
