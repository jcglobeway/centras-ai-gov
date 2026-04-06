/**
 * QA 리뷰 도메인 모델 — 미해결 질문 검토 기록.
 *
 * 리뷰 상태는 PENDING → CONFIRMED_ISSUE|FALSE_ALARM, CONFIRMED_ISSUE → RESOLVED로 전이한다.
 * false_alarm → resolved 전이는 금지되며 이 규칙은 QAReviewStateMachine이 강제한다.
 */
package com.publicplatform.ragops.qareview.domain

import java.time.Instant

enum class QAReviewStatus { PENDING, CONFIRMED_ISSUE, FALSE_ALARM, RESOLVED }
enum class RootCauseCode { MISSING_DOCUMENT, STALE_DOCUMENT, BAD_CHUNKING, RETRIEVAL_FAILURE, GENERATION_ERROR, POLICY_BLOCK, UNCLEAR_QUESTION }
enum class ActionType { FAQ_CREATE, DOCUMENT_FIX_REQUEST, REINDEX_REQUEST, OPS_ISSUE, NO_ACTION }

data class QAReviewSummary(
    val id: String,
    val questionId: String,
    val reviewStatus: QAReviewStatus,
    val rootCauseCode: RootCauseCode?,
    val actionType: ActionType?,
    val actionTargetId: String?,
    val reviewComment: String?,
    val reviewerId: String,
    val assigneeId: String?,
    val reviewedAt: Instant,
)

data class CreateQAReviewCommand(
    val questionId: String,
    val reviewStatus: QAReviewStatus,
    val rootCauseCode: RootCauseCode?,
    val actionType: ActionType?,
    val actionTargetId: String?,
    val reviewComment: String?,
    val reviewerId: String,
    val assigneeId: String? = null,
)
