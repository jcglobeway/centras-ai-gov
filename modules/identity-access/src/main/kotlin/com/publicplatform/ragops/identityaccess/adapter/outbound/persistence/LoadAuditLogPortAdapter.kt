package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.publicplatform.ragops.identityaccess.application.port.out.LoadAuditLogPort
import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

open class LoadAuditLogPortAdapter(
    private val jpaRepository: JpaRecordAuditLogPort,
) : LoadAuditLogPort {

    override fun findRecent(page: Int, pageSize: Int): List<AuditLogEntry> {
        val pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending())
        return jpaRepository.findAll(pageable).content.map { it.toModel() }
    }

    override fun count(): Long = jpaRepository.count()
}
