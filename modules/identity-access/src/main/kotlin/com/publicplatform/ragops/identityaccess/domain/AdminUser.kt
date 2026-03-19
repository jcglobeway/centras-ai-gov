package com.publicplatform.ragops.identityaccess.domain

import java.time.Instant

enum class AdminUserStatus {
    ACTIVE,
    INVITED,
    SUSPENDED,
}

data class AdminUser(
    val id: String,
    val email: String,
    val displayName: String,
    val status: AdminUserStatus,
    val lastLoginAt: Instant?,
)
