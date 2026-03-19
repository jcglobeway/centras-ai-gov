package com.publicplatform.ragops.organizationdirectory.application.port.`in`

import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationScope

/**
 * 기관 목록 조회 인바운드 포트.
 *
 * 세션 권한 범위(OrganizationScope)에 따라 기관 목록을 반환한다.
 * globalAccess=true이면 전체 기관을, false이면 organizationIds에 해당하는 기관만 조회한다.
 */
interface GetOrganizationsUseCase {
    fun listOrganizations(scope: OrganizationScope): List<Organization>
}
