package com.publicplatform.ragops.organizationdirectory.application.port.`in`

import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary

/**
 * 기관 목록 조회 인바운드 포트.
 *
 * 세션 권한 범위에 포함된 기관 목록을 반환한다.
 * ops_admin 역할은 전체 기관을, 나머지 역할은 할당된 기관만 조회할 수 있다.
 */
interface GetOrganizationsUseCase {
    fun getByIds(ids: Set<String>): List<OrganizationSummary>
}
