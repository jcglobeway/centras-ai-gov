package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRecordAuditLogPort : JpaRepository<AuditLogEntity, String>
