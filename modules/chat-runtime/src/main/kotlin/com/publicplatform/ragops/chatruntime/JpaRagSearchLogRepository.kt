package com.publicplatform.ragops.chatruntime

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
