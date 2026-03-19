/**
 * 관리자 행위 감사 로그 저장 아웃바운드 포트.
 *
 * 고위험 액션마다 호출되며, 감사 로그는 삭제하지 않는 append-only 정책을 따른다.
 */
package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry

interface RecordAuditLogPort {
    fun save(entry: AuditLogEntry): AuditLogEntry
}
