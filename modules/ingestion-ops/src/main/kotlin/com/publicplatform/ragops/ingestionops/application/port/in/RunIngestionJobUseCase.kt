package com.publicplatform.ragops.ingestionops.application.port.`in`

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.RequestIngestionJobCommand

/**
 * 인제스션 잡 실행 요청 인바운드 포트.
 *
 * 수동 또는 예약 트리거로 인제스션 잡을 큐에 등록한다.
 */
interface RunIngestionJobUseCase {
    fun execute(command: RequestIngestionJobCommand): IngestionJobSummary
}
