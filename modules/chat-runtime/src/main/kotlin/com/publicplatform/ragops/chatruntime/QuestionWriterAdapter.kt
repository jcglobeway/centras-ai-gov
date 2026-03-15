package com.publicplatform.ragops.chatruntime

import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class QuestionWriterAdapter(
    private val jpaRepository: JpaQuestionRepository,
) : QuestionWriter {

    @Transactional
    override fun createQuestion(command: CreateQuestionCommand): QuestionSummary {
        val id = "question_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = QuestionEntity(
            id = id,
            organizationId = command.organizationId,
            serviceId = command.serviceId,
            chatSessionId = command.chatSessionId,
            questionText = command.questionText,
            questionIntentLabel = command.questionIntentLabel,
            channel = command.channel,
            createdAt = Instant.now(),
        )

        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
