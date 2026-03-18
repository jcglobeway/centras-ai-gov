/**
 * ChatSession DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "chat_sessions")
class ChatSessionEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "organization_id", nullable = false) val organizationId: String,
    @Column(name = "service_id", nullable = false) val serviceId: String,
    @Column(name = "channel", nullable = false) val channel: String,
    @Column(name = "user_key_hash") val userKeyHash: String?,
    @Column(name = "started_at", nullable = false) val startedAt: Instant = Instant.now(),
    @Column(name = "ended_at") val endedAt: Instant?,
    @Column(name = "session_end_type") val sessionEndType: String?,
    @Column(name = "total_question_count", nullable = false) val totalQuestionCount: Int = 0,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)
