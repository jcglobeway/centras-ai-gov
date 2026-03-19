/**
 * 기관 조회 아웃바운드 포트.
 *
 * loadByScope()는 OrganizationScope 기반으로 필터링하여 권한 범위의 기관만 반환한다.
 * getOrganizations()는 ID 집합으로 요약 뷰를 반환하며 권한 체크 없이 내부 참조용으로 사용된다.
 */
package com.publicplatform.ragops.organizationdirectory.application.port.out

import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationScope
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary

interface LoadOrganizationPort {
    fun getOrganizations(ids: Set<String>): List<OrganizationSummary>
    fun listAll(): List<Organization>
    fun loadByScope(scope: OrganizationScope): List<Organization>
}
