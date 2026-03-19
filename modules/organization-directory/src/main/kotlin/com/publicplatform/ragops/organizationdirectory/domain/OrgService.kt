/**
 * 기관 소속 챗봇 서비스 도메인 모델.
 *
 * 하나의 기관은 복수의 서비스(채널)를 운영할 수 있으며,
 * 각 서비스는 고유한 channelType(WEB, KAKAO 등)과 go-live 시각을 갖는다.
 */
package com.publicplatform.ragops.organizationdirectory.domain

import java.time.Instant

data class Service(
    val id: String,
    val organizationId: String,
    val name: String,
    val channelType: String,
    val status: String,
    val goLiveAt: Instant?,
    val createdAt: Instant,
)
