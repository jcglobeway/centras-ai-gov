/**
 * RagRetrievedDocument DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "rag_retrieved_documents")
class RagRetrievedDocumentEntity(
    @Id @Column(name = "id", nullable = false) val id: String,
    @Column(name = "rag_search_log_id", nullable = false) val ragSearchLogId: String,
    @Column(name = "document_id") val documentId: String?,
    @Column(name = "chunk_id") val chunkId: String?,
    @Column(name = "rank", nullable = false) val rank: Int,
    @Column(name = "score", precision = 10, scale = 6) val score: BigDecimal?,
    @Column(name = "used_in_citation", nullable = false) val usedInCitation: Boolean = false,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
)
