package com.publicplatform.ragops.ingestionops.application.service

import com.publicplatform.ragops.ingestionops.application.port.`in`.RunIngestionJobUseCase
import com.publicplatform.ragops.ingestionops.application.port.out.PersistIngestionJobPort
import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.RequestIngestionJobCommand

/**
 * 인제스션 잡 실행 요청 유스케이스 구현체.
 *
 * PersistIngestionJobPort에 위임하여 잡을 큐에 등록한다.
 */
open class RunIngestionJobService(
    private val ingestionJobWriter: PersistIngestionJobPort,
) : RunIngestionJobUseCase {

    override fun execute(command: RequestIngestionJobCommand): IngestionJobSummary =
        ingestionJobWriter.requestJob(command)
}
