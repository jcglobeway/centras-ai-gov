package com.publicplatform.ragops.chatruntime

import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class AnswerWriterAdapter(
    private val jpaRepository: JpaAnswerRepository,
) : AnswerWriter {

    @Transactional
    override fun createAnswer(command: CreateAnswerCommand): AnswerSummary {
        val id = "answer_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = AnswerEntity(
            id = id,
            questionId = command.questionId,
            answerText = command.answerText,
            answerStatus = command.answerStatus.name.lowercase(),
            responseTimeMs = command.responseTimeMs,
            citationCount = command.citationCount,
            fallbackReasonCode = command.fallbackReasonCode,
            createdAt = Instant.now(),
        )

        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
