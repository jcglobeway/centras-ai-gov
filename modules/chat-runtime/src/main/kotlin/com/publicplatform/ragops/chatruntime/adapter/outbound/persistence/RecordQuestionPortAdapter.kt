/**
 * RecordQuestionPort의 JPA 구현체.
 *
 * UUID 기반 ID를 생성하고 질문을 questions 테이블에 저장한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.CreateQuestionCommand
import com.publicplatform.ragops.chatruntime.domain.QuestionSummary
import com.publicplatform.ragops.chatruntime.application.port.out.RecordQuestionPort
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class RecordQuestionPortAdapter(
    private val jpaRepository: JpaQuestionRepository,
) : RecordQuestionPort {

    @Transactional
    override fun createQuestion(command: CreateQuestionCommand): QuestionSummary {
        val id = "question_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = QuestionEntity(
            id = id, organizationId = command.organizationId, serviceId = command.serviceId,
            chatSessionId = command.chatSessionId, questionText = command.questionText,
            questionIntentLabel = command.questionIntentLabel, channel = command.channel,
            questionCategory = command.questionCategory, answerConfidence = null,
            failureReasonCode = command.failureReasonCode?.code, isEscalated = false,
            createdAt = Instant.now(),
        )
        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
