/**
 * 멀티테넌트 기관(Organization) 도메인 모델.
 *
 * 모든 비즈니스 테이블은 organization_id를 통해 이 컨텍스트에 귀속된다.
 * OrganizationScope는 역할별 기관 접근 범위를 표현하며 전체 기관 조회는 globalAccess=true로 표시한다.
 */
package com.publicplatform.ragops.organizationdirectory.domain

import java.time.Instant

data class OrganizationSummary(
    val id: String,
    val name: String,
    val institutionType: String,
)

data class Organization(
    val id: String,
    val name: String,
    val orgCode: String,
    val status: String,
    val institutionType: String,
    val ownerUserId: String?,
    val lastDocumentSyncAt: Instant?,
    val createdAt: Instant,
)

data class OrganizationScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
