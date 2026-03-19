/**
 * organization-directory 바운디드 컨텍스트의 도메인 모델.
 *
 * 멀티테넌트 기관(Organization)과 서비스(Service) 개념을 정의한다.
 * 모든 비즈니스 테이블은 organization_id를 통해 이 컨텍스트에 귀속된다.
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

data class Service(
    val id: String,
    val organizationId: String,
    val name: String,
    val channelType: String,
    val status: String,
    val goLiveAt: Instant?,
    val createdAt: Instant,
)
