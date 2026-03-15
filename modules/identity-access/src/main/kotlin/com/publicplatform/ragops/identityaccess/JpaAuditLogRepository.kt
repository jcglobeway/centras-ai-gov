package com.publicplatform.ragops.identityaccess

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaAuditLogRepository : JpaRepository<AuditLogEntity, String>
