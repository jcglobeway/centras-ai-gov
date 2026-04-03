package com.publicplatform.ragops.adminapi.evaluation.adapter.inbound.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.publicplatform.ragops.chatruntime.domain.QuestionAnsweredEvent
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase

/**
 * answer 트랜잭션 커밋 후 RAGAS 평가 요청을 Redis 큐에 발행하는 어댑터.
 *
 * AFTER_COMMIT 페이즈를 사용하는 이유: 트랜잭션이 롤백되면 평가 요청도 발행하지 않아야 한다.
 * publish 실패는 warn 로그만 남기고 무시한다 — 평가 누락보다 답변 저장 실패가 더 치명적이기 때문이다.
 * 미처리 실패 항목은 eval-runner의 DLQ(ragas:eval:dlq)에서 재처리된다.
 */
open class RagasEvalQueuePublisher(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(RagasEvalQueuePublisher::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: QuestionAnsweredEvent) {
        try {
            val payload = objectMapper.writeValueAsString(
                mapOf(
                    "questionId" to event.questionId,
                    "organizationId" to event.organizationId,
                )
            )
            redisTemplate.opsForList().leftPush("ragas:eval:queue", payload)
        } catch (e: Exception) {
            logger.warn("ragas:eval:queue publish 실패 — questionId={}", event.questionId, e)
        }
    }
}
