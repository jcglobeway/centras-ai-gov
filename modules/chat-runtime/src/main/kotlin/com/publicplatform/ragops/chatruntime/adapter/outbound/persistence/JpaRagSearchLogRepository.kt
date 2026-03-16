/**
 * RagSearchLog 관련 Spring Data JPA 레포지토리.
 *
 * Adapter 클래스에서만 사용하며, RepositoryConfiguration을 통해 주입된다.
 * Controller나 Service가 직접 참조하면 ArchUnit Rule 5가 실패한다.
 */
package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRagSearchLogRepository : JpaRepository<RagSearchLogEntity, String> {
    fun findByQuestionId(questionId: String): List<RagSearchLogEntity>
}

@Repository
interface JpaRagRetrievedDocumentRepository : JpaRepository<RagRetrievedDocumentEntity, String> {
    fun findByRagSearchLogId(ragSearchLogId: String): List<RagRetrievedDocumentEntity>
}
