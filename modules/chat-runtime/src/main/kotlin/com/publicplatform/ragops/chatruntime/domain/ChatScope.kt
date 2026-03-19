/**
 * 채팅 런타임 컨텍스트의 기관 범위 필터.
 *
 * globalAccess=true이면 전체 기관 데이터를 조회하고,
 * false이면 organizationIds에 속한 기관의 데이터만 반환한다.
 */
package com.publicplatform.ragops.chatruntime.domain

data class ChatScope(
    val organizationIds: Set<String>,
    val globalAccess: Boolean,
)
