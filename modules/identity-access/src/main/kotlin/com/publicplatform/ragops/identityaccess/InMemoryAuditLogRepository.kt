package com.publicplatform.ragops.identityaccess

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryAuditLogRepository : AuditLogRepository {
    private val logs = ConcurrentHashMap<String, AuditLogEntry>()
    private val sequenceGenerator = AtomicLong(1)

    override fun save(entry: AuditLogEntry): AuditLogEntry {
        val id = if (entry.id.isEmpty()) {
            "audit_${sequenceGenerator.getAndIncrement()}"
        } else {
            entry.id
        }
        val savedEntry = entry.copy(id = id)
        logs[id] = savedEntry
        return savedEntry
    }
}
