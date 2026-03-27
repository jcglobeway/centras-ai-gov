package com.publicplatform.ragops.chatruntime.domain

import com.publicplatform.ragops.sharedkernel.DomainEvent
import java.time.Instant


data class QuestionAnsweredEvent(
    val questionId: String,
    val organizationId: String,
    val serviceId: String,
    val answerStatus: AnswerStatus,
    val failureReasonCode: String?,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent
