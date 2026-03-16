package com.publicplatform.ragops.chatruntime

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "rag_retrieved_documents")
class RagRetrievedDocumentEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "rag_search_log_id", nullable = false)
    val ragSearchLogId: String,

    @Column(name = "document_id")
    val documentId: String?,

    @Column(name = "chunk_id")
    val chunkId: String?,

    @Column(name = "rank", nullable = false)
    val rank: Int,

    @Column(name = "score", precision = 10, scale = 6)
    val score: BigDecimal?,

    @Column(name = "used_in_citation", nullable = false)
    val usedInCitation: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
