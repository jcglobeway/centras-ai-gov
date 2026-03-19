/**
 * 관리자 세션 도메인 모델 — 로그인 상태와 권한 스냅샷.
 *
 * AdminSessionSnapshot은 로그인 시점의 역할·권한을 담은 불변 스냅샷으로,
 * 세션 유효 기간(기본 8시간, defaultAdminSessionDuration()) 동안 재사용된다.
 * isUsableAt()은 세션 만료 및 폐기 여부를 확인하는 도메인 함수이다.
 */
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
