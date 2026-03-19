/**
 * 관리자 이메일·비밀번호 인증 아웃바운드 포트.
 *
 * null 반환은 자격증명 불일치를 의미하며 AUTH_INVALID_CREDENTIALS 에러로 처리된다.
 * 구현체는 DevelopmentAdminCredentialAuthenticator(고정 자격증명)로 시작하여
 * 프로덕션에서는 DB 기반 구현체로 교체할 수 있도록 포트로 분리한다.
 */
package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AuthenticatedAdminPrincipal

interface AdminCredentialAuthenticator {
    fun authenticate(email: String, password: String): AuthenticatedAdminPrincipal?
}
