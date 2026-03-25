package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.UpdateChatSessionPort
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

open class UpdateChatSessionPortAdapter(
    private val jpaRepository: JpaChatSessionRepository,
) : UpdateChatSessionPort {

    @Transactional
    override fun incrementQuestionCount(sessionId: String) {
        jpaRepository.incrementQuestionCount(sessionId)
    }

    @Transactional
    override fun updateSessionEndType(sessionId: String, endType: String) {
        jpaRepository.updateSessionEndType(sessionId, endType, Instant.now())
    }
}
