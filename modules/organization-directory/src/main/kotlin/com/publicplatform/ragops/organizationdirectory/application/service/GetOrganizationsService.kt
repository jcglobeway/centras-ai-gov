package com.publicplatform.ragops.organizationdirectory.application.service

import com.publicplatform.ragops.organizationdirectory.application.port.`in`.GetOrganizationsUseCase
import com.publicplatform.ragops.organizationdirectory.application.port.out.LoadOrganizationPort
import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationScope

/**
 * 기관 목록 조회 유스케이스 구현체.
 *
 * OrganizationScope를 LoadOrganizationPort에 전달하여 스코프 판정을 서비스 레이어에서 처리한다.
 */
open class GetOrganizationsService(
    private val organizationDirectoryReader: LoadOrganizationPort,
) : GetOrganizationsUseCase {

    override fun listOrganizations(scope: OrganizationScope): List<Organization> =
        organizationDirectoryReader.loadByScope(scope)
}
