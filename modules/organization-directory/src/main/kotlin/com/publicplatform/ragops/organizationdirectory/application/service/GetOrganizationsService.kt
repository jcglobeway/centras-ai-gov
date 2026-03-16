package com.publicplatform.ragops.organizationdirectory.application.service

import com.publicplatform.ragops.organizationdirectory.application.port.`in`.GetOrganizationsUseCase
import com.publicplatform.ragops.organizationdirectory.application.port.out.LoadOrganizationPort
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary

/**
 * 기관 목록 조회 유스케이스 구현체.
 *
 * LoadOrganizationPort에 위임하여 세션 범위 내 기관 목록을 반환한다.
 */
open class GetOrganizationsService(
    private val organizationDirectoryReader: LoadOrganizationPort,
) : GetOrganizationsUseCase {

    override fun getByIds(ids: Set<String>): List<OrganizationSummary> =
        organizationDirectoryReader.getOrganizations(ids)
}
