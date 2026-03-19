/**
 * 관리자 세션 발급·조회·폐기 아웃바운드 포트.
 *
 * revoke()는 revokedAt 시각을 기록하며 실제 레코드 삭제는 하지 않는다.
 * 폐기된 세션은 isUsableAt() 체크에서 거부된다.
 */
package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AdminSessionIssueCommand
import com.publicplatform.ragops.identityaccess.domain.AdminSessionRecord
import java.time.Instant

interface ManageAdminSessionPort {
    fun findBySessionId(sessionId: String): AdminSessionRecord?
    fun issue(command: AdminSessionIssueCommand): AdminSessionRecord
    fun revoke(sessionId: String, revokedAt: Instant): AdminSessionRecord?
}
