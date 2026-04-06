package com.publicplatform.ragops.chatruntime.application.port.`in`

import com.publicplatform.ragops.chatruntime.domain.RagSearchLogStats

interface GetRagSearchLogStatsUseCase {
    fun getStats(orgId: String?, fromDate: String?, toDate: String?): RagSearchLogStats
}
