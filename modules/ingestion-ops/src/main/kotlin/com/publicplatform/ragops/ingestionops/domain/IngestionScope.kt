/**
 * 인제스션 컨텍스트의 기관 범위 필터.
 *
 * globalAccess=true이면 전체 기관 잡을 조회하고,
 * false이면 organizationIds에 속한 기관의 잡만 반환한다.
 */
package com.publicplatform.ragops.ingestionops.domain

data class IngestionScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
