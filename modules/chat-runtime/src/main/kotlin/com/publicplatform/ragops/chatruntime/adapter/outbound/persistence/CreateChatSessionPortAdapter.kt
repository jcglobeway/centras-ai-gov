package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.CreateChatSessionPort
import java.time.Instant
import java.util.UUID

open class CreateChatSessionPortAdapter(
    private val jpaRepository: JpaChatSessionRepository,
) : CreateChatSessionPort {
    override fun create(organizationId: String, serviceId: String, channel: String): String {
        val id = "sim_sess_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        jpaRepository.save(
            ChatSessionEntity(
                id = id,
                organizationId = organizationId,
                serviceId = serviceId,
                channel = channel,
                userKeyHash = null,
                startedAt = Instant.now(),
                endedAt = null,
                sessionEndType = null,
                totalQuestionCount = 0,
                createdAt = Instant.now(),
            )
        )
        return id
    }
}
