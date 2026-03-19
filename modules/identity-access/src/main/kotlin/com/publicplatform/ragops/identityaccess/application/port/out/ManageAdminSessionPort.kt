package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AdminSessionIssueCommand
import com.publicplatform.ragops.identityaccess.domain.AdminSessionRecord
import java.time.Instant

interface ManageAdminSessionPort {
    fun findBySessionId(sessionId: String): AdminSessionRecord?
    fun issue(command: AdminSessionIssueCommand): AdminSessionRecord
    fun revoke(sessionId: String, revokedAt: Instant): AdminSessionRecord?
}
