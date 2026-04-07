package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.AnswerCorrectionSummary
import com.publicplatform.ragops.chatruntime.domain.CorrectionScope
import com.publicplatform.ragops.chatruntime.domain.CreateCorrectionCommand

interface ManageCorrectionUseCase {
    fun create(command: CreateCorrectionCommand): AnswerCorrectionSummary
    fun list(scope: CorrectionScope): List<AnswerCorrectionSummary>
}
