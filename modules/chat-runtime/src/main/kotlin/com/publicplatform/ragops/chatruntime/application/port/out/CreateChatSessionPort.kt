package com.publicplatform.ragops.chatruntime.application.port.out

interface CreateChatSessionPort {
    fun create(organizationId: String, serviceId: String, channel: String): String
}
