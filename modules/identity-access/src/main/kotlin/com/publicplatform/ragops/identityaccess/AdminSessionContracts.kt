package com.publicplatform.ragops.identityaccess

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
    val lastLoginAt: Instant,
)

data class AdminRoleAssignment(
    val roleCode: String,
    val organizationId: String?,
)

data class AdminSessionSnapshot(
    val user: AdminUser,
    val roleAssignments: List<AdminRoleAssignment>,
    val grantedActions: List<String>,
)

data class SessionLookup(
    val userIdHint: String?,
    val emailHint: String?,
    val displayNameHint: String?,
    val roleCodeHint: String?,
    val organizationIdHint: String?,
)

interface AdminSessionReader {
    fun restoreSession(lookup: SessionLookup): AdminSessionSnapshot
}
