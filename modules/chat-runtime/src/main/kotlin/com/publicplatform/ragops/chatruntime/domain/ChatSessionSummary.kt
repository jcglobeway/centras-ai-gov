/**
 * 시민 채팅 세션의 요약 뷰.
 *
 * 세션 시작·종료 시각과 총 질문 수를 제공하며,
 * userKeyHash로 동일 사용자 재방문 여부를 추적한다 (PII 보호를 위해 해시 처리).
 */
package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class ChatSessionSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val channel: String,
    val userKeyHash: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val sessionEndType: String?,
    val totalQuestionCount: Int,
)
