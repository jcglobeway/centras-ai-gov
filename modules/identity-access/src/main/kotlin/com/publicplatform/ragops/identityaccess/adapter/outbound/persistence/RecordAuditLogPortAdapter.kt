/**
 * RecordAuditLogPort의 JPA 구현체.
 *
 * 고위험 관리자 액션을 audit_logs 테이블에 기록한다.
 */
package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry
import com.publicplatform.ragops.identityaccess.application.port.out.RecordAuditLogPort
import java.util.UUID

open class RecordAuditLogPortAdapter(
    private val jpaRepository: JpaRecordAuditLogPort,
) : RecordAuditLogPort {

    override fun save(entry: AuditLogEntry): AuditLogEntry {
        val id = if (entry.id.isEmpty()) UUID.randomUUID().toString() else entry.id
        val savedEntry = entry.copy(id = id)
        val entity = savedEntry.toEntity()
        jpaRepository.save(entity)
        return savedEntry
    }
}
