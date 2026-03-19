/**
 * 시민 피드백 목록 조회 아웃바운드 포트.
 *
 * FeedbackScope로 기관 범위를 필터링하며, 피드백 분석 대시보드에서 사용된다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.FeedbackScope
import com.publicplatform.ragops.chatruntime.domain.FeedbackSummary

interface LoadFeedbackPort {
    fun listFeedbacks(scope: FeedbackScope): List<FeedbackSummary>
}
