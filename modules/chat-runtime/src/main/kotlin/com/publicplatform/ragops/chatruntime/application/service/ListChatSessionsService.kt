package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.ListChatSessionsUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadChatSessionPort
import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.ChatSessionSummary

open class ListChatSessionsService(
    private val sessionReader: LoadChatSessionPort,
) : ListChatSessionsUseCase {

    override fun listSessions(scope: ChatScope, from: String?, to: String?): List<ChatSessionSummary> =
        sessionReader.listSessions(scope, from, to)

    override fun getSession(sessionId: String): ChatSessionSummary? =
        sessionReader.getSession(sessionId)
}
