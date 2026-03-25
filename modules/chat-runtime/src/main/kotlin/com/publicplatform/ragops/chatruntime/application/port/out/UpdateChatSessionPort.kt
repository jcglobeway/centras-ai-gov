package com.publicplatform.ragops.chatruntime.application.port.out

interface UpdateChatSessionPort {
    fun incrementQuestionCount(sessionId: String)
    fun updateSessionEndType(sessionId: String, endType: String)
}
