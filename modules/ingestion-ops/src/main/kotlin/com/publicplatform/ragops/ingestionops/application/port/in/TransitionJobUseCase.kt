package com.publicplatform.ragops.ingestionops.application.port.`in`

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.TransitionIngestionJobCommand

/**
 * 인제스션 잡 상태 전이 인바운드 포트.
 *
 * Python 워커가 잡 진행 상황을 콜백할 때 호출된다.
 * 상태 머신 검증은 IngestionJobStateMachine에 위임한다.
 */
interface TransitionJobUseCase {
    fun execute(command: TransitionIngestionJobCommand): IngestionJobSummary
}
