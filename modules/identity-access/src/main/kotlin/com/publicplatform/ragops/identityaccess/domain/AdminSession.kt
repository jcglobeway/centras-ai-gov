package com.publicplatform.ragops.identityaccess.domain

import java.time.Duration
import java.time.Instant

data class AdminRoleAssignment(
    val roleCode: String,
    val organizationId: String?,
)

data class AdminSessionSnapshot(
    val user: AdminUser,
    val roleAssignments: List<AdminRoleAssignment>,
    val grantedActions: List<String>,
)

data class AdminSessionRecord(
    val sessionId: String,
    val snapshot: AdminSessionSnapshot,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val lastSeenAt: Instant,
    val revokedAt: Instant?,
)

data class AdminSessionIssueCommand(
    val snapshot: AdminSessionSnapshot,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val userAgent: String?,
    val ipAddress: String?,
)

data class SessionLookup(
    val sessionId: String?,
    val userIdHint: String?,
    val emailHint: String?,
    val displayNameHint: String?,
    val roleCodeHint: String?,
    val organizationIdHint: String?,
)

fun AdminSessionRecord.isUsableAt(at: Instant): Boolean =
    revokedAt == null && expiresAt.isAfter(at)

fun defaultAdminSessionDuration(): Duration = Duration.ofHours(8)
