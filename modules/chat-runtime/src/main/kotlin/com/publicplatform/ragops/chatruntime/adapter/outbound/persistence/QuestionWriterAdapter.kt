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
            createdAt = Instant.now(),
        )
        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
