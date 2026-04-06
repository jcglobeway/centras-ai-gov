package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.ChatScope
import com.publicplatform.ragops.chatruntime.domain.ChatSessionSummary

interface ListChatSessionsUseCase {
    fun listSessions(scope: ChatScope, from: String?, to: String?): List<ChatSessionSummary>
    fun getSession(sessionId: String): ChatSessionSummary?
}
