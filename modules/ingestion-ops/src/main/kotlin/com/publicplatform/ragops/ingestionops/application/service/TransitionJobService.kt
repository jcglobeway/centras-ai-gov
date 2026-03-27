package com.publicplatform.ragops.ingestionops.application.service

import com.publicplatform.ragops.ingestionops.application.port.`in`.TransitionJobUseCase
import com.publicplatform.ragops.ingestionops.application.port.out.PersistIngestionJobPort
import com.publicplatform.ragops.ingestionops.domain.IngestionJobCompletedEvent
import com.publicplatform.ragops.ingestionops.domain.IngestionJobStatus
import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.TransitionIngestionJobCommand
import org.springframework.context.ApplicationEventPublisher

/**
 * 인제스션 잡 상태 전이 유스케이스 구현체.
 *
 * PersistIngestionJobPort에 위임하며, 상태 머신 검증은 PersistIngestionJobPortAdapter 내에서 수행된다.
 */
open class TransitionJobService(
    private val ingestionJobWriter: PersistIngestionJobPort,
    private val eventPublisher: ApplicationEventPublisher,
) : TransitionJobUseCase {

    override fun execute(command: TransitionIngestionJobCommand): IngestionJobSummary {
        val result = ingestionJobWriter.transitionJob(command)
        if (result.status == IngestionJobStatus.SUCCEEDED || result.status == IngestionJobStatus.FAILED) {
            eventPublisher.publishEvent(
                IngestionJobCompletedEvent(
                    jobId = result.id,
                    organizationId = result.organizationId,
                    serviceId = result.serviceId,
                    success = result.status == IngestionJobStatus.SUCCEEDED,
                ),
            )
        }
        return result
    }
}
