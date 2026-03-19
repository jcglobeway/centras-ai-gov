/**
 * RecordAnswerPort의 JPA 구현체.
 *
 * 질문에 대한 RAG 또는 수동 답변을 answers 테이블에 저장한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.domain.AnswerSummary
import com.publicplatform.ragops.chatruntime.domain.CreateAnswerCommand
import com.publicplatform.ragops.chatruntime.application.port.out.RecordAnswerPort
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class RecordAnswerPortAdapter(
    private val jpaRepository: JpaAnswerRepository,
) : RecordAnswerPort {

    @Transactional
    override fun createAnswer(command: CreateAnswerCommand): AnswerSummary {
        val id = "answer_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = AnswerEntity(
            id = id, questionId = command.questionId, answerText = command.answerText,
            answerStatus = command.answerStatus.name.lowercase(), responseTimeMs = command.responseTimeMs,
            citationCount = command.citationCount, fallbackReasonCode = command.fallbackReasonCode,
            createdAt = Instant.now(),
        )
        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
