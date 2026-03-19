/**
 * 관리자 사용자 조회 및 저장 아웃바운드 포트.
 *
 * 로그인 흐름에서 이메일로 사용자를 조회하고, 마지막 로그인 시각 갱신 시 save()를 호출한다.
 */
package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AdminUser

interface ManageAdminUserPort {
    fun findByEmail(email: String): AdminUser?
    fun findById(userId: String): AdminUser?
    fun save(user: AdminUser): AdminUser
}
