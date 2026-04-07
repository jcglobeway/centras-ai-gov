package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.ManageCorrectionUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadCorrectionPort
import com.publicplatform.ragops.chatruntime.application.port.out.RecordCorrectionPort
import com.publicplatform.ragops.chatruntime.domain.AnswerCorrectionSummary
import com.publicplatform.ragops.chatruntime.domain.CorrectionScope
import com.publicplatform.ragops.chatruntime.domain.CreateCorrectionCommand

open class ManageCorrectionService(
    private val correctionWriter: RecordCorrectionPort,
    private val correctionReader: LoadCorrectionPort,
) : ManageCorrectionUseCase {

    override fun create(command: CreateCorrectionCommand): AnswerCorrectionSummary =
        correctionWriter.createCorrection(command)

    override fun list(scope: CorrectionScope): List<AnswerCorrectionSummary> =
        correctionReader.listCorrections(scope)
}
