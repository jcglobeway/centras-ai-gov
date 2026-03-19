/**
 * HTTP 요청에서 세션을 복원하는 아웃바운드 포트.
 *
 * X-Admin-Session-Id 헤더 또는 디버그 힌트 헤더로 세션을 조회하며,
 * 세션이 없거나 만료된 경우 AdminAuthenticationException을 던진다.
 */
package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.SessionLookup

interface RestoreSessionPort {
    fun restoreSession(lookup: SessionLookup): AdminSessionSnapshot
}
