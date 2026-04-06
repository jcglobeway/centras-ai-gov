/**
 * LoadQuestionPort의 JPA 구현체.
 *
 * 미해결 질문 큐 조회는 qa_reviews 테이블과 Native SQL을 사용한다.
 * 순환 의존성을 피하기 위해 qa-review 모듈을 import하지 않고 네이티브 쿼리로 처리한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.FailureReasonCode
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary
import com.publicplatform.ragops.chatruntime.domain.UnresolvedQuestionSummary
import com.publicplatform.ragops.chatruntime.application.port.out.LoadQuestionPort

open class LoadQuestionPortAdapter(
    private val jpaRepository: JpaQuestionRepository,
) : LoadQuestionPort {

    override fun listQuestions(scope: ChatScope): List<QuestionSummary> {
        val allQuestions = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) allQuestions
        else allQuestions.filter { it.organizationId in scope.organizationIds }
    }

    override fun listQuestionsWithAnswers(scope: ChatScope): List<QuestionSummary> {
        val rows = jpaRepository.findAllWithAnswers().map { row ->
            QuestionSummary(
                id = row.getQuestionId(),
                organizationId = row.getOrganizationId(),
                serviceId = row.getServiceId(),
                chatSessionId = row.getChatSessionId(),
                questionText = row.getQuestionText(),
                questionIntentLabel = row.getQuestionIntentLabel(),
                channel = row.getChannel(),
                questionCategory = row.getQuestionCategory(),
                answerConfidence = row.getAnswerConfidence(),
                failureReasonCode = FailureReasonCode.fromCodeOrNull(row.getFailureReasonCode()),
                isEscalated = row.getIsEscalated(),
                createdAt = row.getCreatedAt(),
                answerText = row.getAnswerText(),
                answerStatus = row.getAnswerStatus(),
                responseTimeMs = row.getResponseTimeMs(),
                faithfulness = row.getFaithfulness(),
                answerRelevancy = row.getAnswerRelevancy(),
                contextPrecision = row.getContextPrecision(),
                contextRecall = row.getContextRecall(),
            )
        }
        return if (scope.globalAccess) rows
        else rows.filter { it.organizationId in scope.organizationIds }
    }

    override fun listQuestionsWithAnswersBySession(chatSessionId: String): List<QuestionSummary> =
        jpaRepository.findAllWithAnswersBySessionId(chatSessionId).map { row ->
            QuestionSummary(
                id = row.getQuestionId(),
                organizationId = row.getOrganizationId(),
                serviceId = row.getServiceId(),
                chatSessionId = row.getChatSessionId(),
                questionText = row.getQuestionText(),
                questionIntentLabel = row.getQuestionIntentLabel(),
                channel = row.getChannel(),
                questionCategory = row.getQuestionCategory(),
                answerConfidence = row.getAnswerConfidence(),
                failureReasonCode = FailureReasonCode.fromCodeOrNull(row.getFailureReasonCode()),
                isEscalated = row.getIsEscalated(),
                createdAt = row.getCreatedAt(),
                answerText = row.getAnswerText(),
                answerStatus = row.getAnswerStatus(),
                responseTimeMs = row.getResponseTimeMs(),
                faithfulness = row.getFaithfulness(),
                answerRelevancy = row.getAnswerRelevancy(),
                contextPrecision = row.getContextPrecision(),
                contextRecall = row.getContextRecall(),
            )
        }

    override fun listUnresolvedQuestions(scope: ChatScope): List<UnresolvedQuestionSummary> {
        val rows = jpaRepository.findUnresolvedWithStatus().map { row ->
            UnresolvedQuestionSummary(
                questionId = row.getQuestionId(),
                organizationId = row.getOrganizationId(),
                questionText = row.getQuestionText(),
                failureReasonCode = FailureReasonCode.fromCodeOrNull(row.getFailureReasonCode()),
                questionCategory = row.getQuestionCategory(),
                isEscalated = row.getIsEscalated(),
                answerStatus = row.getAnswerStatus(),
                latestReviewStatus = row.getLatestReviewStatus(),
                latestReviewId = row.getLatestReviewId(),
                latestReviewAssigneeId = row.getLatestReviewAssigneeId(),
                createdAt = row.getCreatedAt(),
            )
        }
        return if (scope.globalAccess) rows
        else rows.filter { it.organizationId in scope.organizationIds }
    }
}
