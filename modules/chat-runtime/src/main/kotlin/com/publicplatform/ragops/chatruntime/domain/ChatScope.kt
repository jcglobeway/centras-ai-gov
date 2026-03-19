package com.publicplatform.ragops.chatruntime.domain

data class ChatScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
