package com.publicplatform.ragops.ingestionops.application.service

import com.publicplatform.ragops.ingestionops.application.port.`in`.TransitionJobUseCase
import com.publicplatform.ragops.ingestionops.application.port.out.PersistIngestionJobPort
import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.TransitionIngestionJobCommand

/**
 * 인제스션 잡 상태 전이 유스케이스 구현체.
 *
 * PersistIngestionJobPort에 위임하며, 상태 머신 검증은 PersistIngestionJobPortAdapter 내에서 수행된다.
 */
open class TransitionJobService(
    private val ingestionJobWriter: PersistIngestionJobPort,
) : TransitionJobUseCase {

    override fun execute(command: TransitionIngestionJobCommand): IngestionJobSummary =
        ingestionJobWriter.transitionJob(command)
}
