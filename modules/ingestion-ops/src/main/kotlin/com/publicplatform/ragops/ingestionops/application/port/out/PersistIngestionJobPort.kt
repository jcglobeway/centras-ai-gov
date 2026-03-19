/**
 * 인제스션 잡 생성·상태 전이 아웃바운드 포트.
 *
 * transitionJob()은 IngestionJobStateMachine 검증 이후 호출되어야 하며,
 * 상태 전이 유효성 검증은 서비스 계층 책임이다.
 */
package com.publicplatform.ragops.ingestionops.application.port.out

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.RequestIngestionJobCommand
import com.publicplatform.ragops.ingestionops.domain.TransitionIngestionJobCommand

interface PersistIngestionJobPort {
    fun requestJob(command: RequestIngestionJobCommand): IngestionJobSummary
    fun transitionJob(command: TransitionIngestionJobCommand): IngestionJobSummary
}
