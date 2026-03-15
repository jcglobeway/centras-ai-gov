package com.publicplatform.ragops.identityaccess

import java.util.UUID

open class AuditLogRepositoryAdapter(
    private val jpaRepository: JpaAuditLogRepository,
) : AuditLogRepository {

    override fun save(entry: AuditLogEntry): AuditLogEntry {
        val id = if (entry.id.isEmpty()) UUID.randomUUID().toString() else entry.id
        val savedEntry = entry.copy(id = id)
        val entity = savedEntry.toEntity()
        jpaRepository.save(entity)
        return savedEntry
    }
}
