/**
 * 시민 피드백 도메인 모델 — 행동 신호 포함.
 *
 * rating(1~5) 외에 clickedLink, clickedDocument, dwellTimeMs 등 묵시적 행동 신호를 추적한다.
 * FeedbackScope는 기관별 필터링에 사용되며 ChatScope와 동일한 패턴을 따른다.
 */
package com.publicplatform.ragops.chatruntime.domain

import java.time.Instant

data class FeedbackSummary(
    val id: String,
    val organizationId: String,
    val serviceId: String,
    val questionId: String?,
    val sessionId: String?,
    val rating: Int,
    val comment: String?,
    val channel: String?,
    val feedbackType: String?,
    val clickedLink: Boolean,
    val clickedDocument: Boolean,
    val targetActionType: String?,
    val targetActionCompleted: Boolean,
    val dwellTimeMs: Long?,
    val submittedAt: Instant,
)

data class CreateFeedbackCommand(
    val organizationId: String,
    val serviceId: String,
    val questionId: String?,
    val sessionId: String?,
    val rating: Int,
    val comment: String?,
    val channel: String?,
    val feedbackType: String? = null,
    val clickedLink: Boolean = false,
    val clickedDocument: Boolean = false,
    val targetActionType: String? = null,
    val targetActionCompleted: Boolean = false,
    val dwellTimeMs: Long? = null,
)

data class FeedbackScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
