/**
 * 관리자 행위 감사 로그 엔트리.
 *
 * 고위험 액션(로그인, 잡 취소, 역할 변경 등)은 반드시 이 엔트리를 생성해야 한다.
 * requestId와 traceId를 포함하여 분산 트레이싱과 연계할 수 있다.
 */
package com.publicplatform.ragops.identityaccess.domain

import java.time.Instant

data class AuditLogEntry(
    val id: String,
    val actorUserId: String?,
    val actorRoleCode: String?,
    val organizationId: String?,
    val actionCode: String,
    val resourceType: String?,
    val resourceId: String?,
    val requestId: String?,
    val traceId: String?,
    val resultCode: String,
    val createdAt: Instant,
)
