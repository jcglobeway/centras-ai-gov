package com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaRagasEvaluationRepository : JpaRepository<RagasEvaluationEntity, String>
