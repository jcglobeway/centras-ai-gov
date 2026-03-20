package com.publicplatform.ragops.adminapi.evaluation.application.port.`in`

import com.publicplatform.ragops.adminapi.evaluation.domain.RagasEvaluationSummary

interface RecordRagasEvaluationUseCase {
    fun record(command: RecordRagasEvaluationCommand): RagasEvaluationSummary
}
