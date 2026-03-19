/**
 * 인제스션 잡 목록 조회 아웃바운드 포트.
 *
 * IngestionScope로 기관 범위를 필터링하여 권한 범위의 잡만 반환한다.
 */
package com.publicplatform.ragops.ingestionops.application.port.out

import com.publicplatform.ragops.ingestionops.domain.IngestionJobSummary
import com.publicplatform.ragops.ingestionops.domain.IngestionScope

interface LoadIngestionJobPort {
    fun listJobs(scope: IngestionScope): List<IngestionJobSummary>
}
