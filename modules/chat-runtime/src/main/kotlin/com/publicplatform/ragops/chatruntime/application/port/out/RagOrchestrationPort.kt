/**
 * RAG 오케스트레이터 호출 아웃바운드 포트.
 *
 * null 반환은 RAG 비활성(rag.orchestrator.enabled=false) 또는 호출 실패를 의미하며,
 * 이 경우 서비스 계층은 FALLBACK 상태의 답변을 생성한다.
 */
package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.RagAnswerResult

interface RagOrchestrationPort {
    fun generateAnswer(
        questionId: String,
        questionText: String,
        organizationId: String,
        serviceId: String,
    ): RagAnswerResult?
}
