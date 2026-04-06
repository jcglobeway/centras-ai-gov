package com.publicplatform.ragops.chatruntime.application.service

import com.publicplatform.ragops.chatruntime.application.port.`in`.GetRagSearchLogStatsUseCase
import com.publicplatform.ragops.chatruntime.application.port.out.LoadRagSearchLogPort
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogStats

open class GetRagSearchLogStatsService(
    private val loadRagSearchLogPort: LoadRagSearchLogPort,
) : GetRagSearchLogStatsUseCase {

    override fun getStats(orgId: String?, fromDate: String?, toDate: String?): RagSearchLogStats {
        val logs = loadRagSearchLogPort.listLogsByOrganization(orgId, fromDate, toDate)
        if (logs.isEmpty()) {
            return RagSearchLogStats(
                total = 0, avgLatencyMs = null, p50LatencyMs = null, p95LatencyMs = null,
                zeroResultRate = 0.0, avgTopK = null, retrievalStatusDistribution = emptyMap(),
            )
        }

        val latencies = logs.mapNotNull { it.latencyMs }.sorted()
        val avgLatencyMs = if (latencies.isNotEmpty()) latencies.average() else null
        val p50LatencyMs = percentile(latencies, 50)
        val p95LatencyMs = percentile(latencies, 95)
        val zeroResultRate = logs.count { it.zeroResult }.toDouble() / logs.size
        val avgTopK = logs.mapNotNull { it.topK }.let { if (it.isNotEmpty()) it.average() else null }
        val retrievalStatusDistribution = logs.groupingBy { it.retrievalStatus }.eachCount()

        return RagSearchLogStats(
            total = logs.size,
            avgLatencyMs = avgLatencyMs,
            p50LatencyMs = p50LatencyMs,
            p95LatencyMs = p95LatencyMs,
            zeroResultRate = zeroResultRate,
            avgTopK = avgTopK,
            retrievalStatusDistribution = retrievalStatusDistribution,
        )
    }

    private fun percentile(sorted: List<Int>, pct: Int): Int? {
        if (sorted.isEmpty()) return null
        val idx = (sorted.size * pct / 100.0).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }
}
