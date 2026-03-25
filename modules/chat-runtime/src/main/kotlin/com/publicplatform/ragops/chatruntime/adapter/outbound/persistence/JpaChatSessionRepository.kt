package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface JpaChatSessionRepository : JpaRepository<ChatSessionEntity, String> {
    @Modifying
    @Query("""
        UPDATE ChatSessionEntity s SET
          s.totalQuestionCount = s.totalQuestionCount + 1
        WHERE s.id = :id
    """)
    fun incrementQuestionCount(@Param("id") id: String)

    @Modifying
    @Query("""
        UPDATE ChatSessionEntity s SET
          s.sessionEndType = :endType,
          s.endedAt = :endedAt
        WHERE s.id = :id
    """)
    fun updateSessionEndType(
        @Param("id") id: String,
        @Param("endType") endType: String,
        @Param("endedAt") endedAt: Instant,
    )
}
