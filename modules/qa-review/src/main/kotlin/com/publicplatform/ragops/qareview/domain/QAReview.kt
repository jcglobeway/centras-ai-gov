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
)
