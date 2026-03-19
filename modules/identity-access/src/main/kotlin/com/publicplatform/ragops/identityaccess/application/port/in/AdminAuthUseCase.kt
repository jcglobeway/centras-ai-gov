package com.publicplatform.ragops.identityaccess.application.port.`in`

import com.publicplatform.ragops.identityaccess.domain.AdminLoginCommand
import com.publicplatform.ragops.identityaccess.domain.AdminLoginResult

/**
 * 관리자 인증 인바운드 포트.
 *
 * 로그인(세션 발급)과 로그아웃(세션 폐기)을 추상화한다.
 * 구현체는 DevelopmentAdminSessionService이며, 운영 환경에서는 DB 기반 구현체로 교체 가능하다.
 */
interface AdminAuthUseCase {
    fun login(command: AdminLoginCommand): AdminLoginResult
    fun logout(sessionId: String)
}
