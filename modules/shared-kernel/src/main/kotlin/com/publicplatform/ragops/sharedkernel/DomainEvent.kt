/**
 * 모든 도메인 이벤트의 공통 마커 인터페이스.
 *
 * 현재는 마커 역할만 하며, 이벤트 발행 인프라가 추가될 때 공통 메타데이터를 정의할 예정이다.
 */
package com.publicplatform.ragops.sharedkernel

import java.time.Instant

interface DomainEvent {
    val occurredAt: Instant
}

