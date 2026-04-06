package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.LoadChatSessionPort
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.ChatSessionSummary
import java.time.LocalDate
import java.time.ZoneOffset

open class LoadChatSessionPortAdapter(
    private val jpaRepository: JpaChatSessionRepository,
) : LoadChatSessionPort {

    override fun listSessions(scope: ChatScope, from: String?, to: String?): List<ChatSessionSummary> {
        val fromInst = from?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant() }
        val toInst = to?.let { LocalDate.parse(it).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() }

        val sessions = if (scope.globalAccess) jpaRepository.findAllByOrderByStartedAtDesc()
                       else jpaRepository.findByOrganizationIdInOrderByStartedAtDesc(scope.organizationIds)

        return sessions
            .filter { (fromInst == null || it.startedAt >= fromInst) && (toInst == null || it.startedAt < toInst) }
            .map { it.toSummary() }
    }

    override fun getSession(sessionId: String): ChatSessionSummary? =
        jpaRepository.findById(sessionId).orElse(null)?.toSummary()

    private fun ChatSessionEntity.toSummary() = ChatSessionSummary(
        id = id,
        organizationId = organizationId,
        serviceId = serviceId,
        channel = channel,
        userKeyHash = userKeyHash,
        startedAt = startedAt,
        endedAt = endedAt,
        sessionEndType = sessionEndType,
        totalQuestionCount = totalQuestionCount,
    )
}
