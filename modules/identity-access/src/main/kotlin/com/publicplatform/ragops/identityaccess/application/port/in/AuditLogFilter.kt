package com.publicplatform.ragops.identityaccess.application.port.`in`

import java.time.LocalDate

data class AuditLogFilter(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val actionCode: String? = null,
    val organizationId: String? = null,
    val actorUserId: String? = null,
)
