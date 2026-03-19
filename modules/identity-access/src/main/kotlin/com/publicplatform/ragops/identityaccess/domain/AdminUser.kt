/**
 * 관리자 사용자 도메인 모델.
 *
 * INVITED 상태 사용자는 초대 이메일 수락 전으로 로그인할 수 없다.
 * SUSPENDED는 계정 잠금 상태이며 운영자만 복구할 수 있다.
 */
package com.publicplatform.ragops.identityaccess.domain

import java.time.Instant

enum class AdminUserStatus {
    ACTIVE,
    INVITED,
    SUSPENDED,
}

data class AdminUser(
    val id: String,
    val email: String,
    val displayName: String,
    val status: AdminUserStatus,
    val lastLoginAt: Instant?,
)
