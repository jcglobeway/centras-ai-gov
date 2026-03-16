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
