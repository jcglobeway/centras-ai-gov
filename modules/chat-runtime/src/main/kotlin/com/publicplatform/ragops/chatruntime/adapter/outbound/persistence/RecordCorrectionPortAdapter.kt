package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

import com.publicplatform.ragops.chatruntime.application.port.out.RecordCorrectionPort
import com.publicplatform.ragops.chatruntime.domain.AnswerCorrectionSummary
import com.publicplatform.ragops.chatruntime.domain.CreateCorrectionCommand
import java.util.UUID

open class RecordCorrectionPortAdapter(
    private val jpaRepository: JpaAnswerCorrectionRepository,
) : RecordCorrectionPort {

    override fun createCorrection(command: CreateCorrectionCommand): AnswerCorrectionSummary {
        val entity = AnswerCorrectionEntity(
            id = "correction_${UUID.randomUUID().toString().substring(0, 8)}",
            organizationId = command.organizationId,
            serviceId = command.serviceId,
            questionId = command.questionId,
            questionText = command.questionText,
            originalAnswerText = command.originalAnswerText,
            correctedAnswerText = command.correctedAnswerText,
            correctedBy = command.correctedBy,
            correctionReason = command.correctionReason,
        )
        return jpaRepository.save(entity).toSummary()
    }
}
