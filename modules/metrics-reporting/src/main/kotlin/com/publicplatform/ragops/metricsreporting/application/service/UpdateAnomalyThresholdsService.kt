package com.publicplatform.ragops.metricsreporting.application.service

import com.publicplatform.ragops.metricsreporting.application.port.`in`.UpdateAnomalyThresholdsUseCase
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveAnomalyThresholdPort
import com.publicplatform.ragops.metricsreporting.domain.AnomalyThreshold
import com.publicplatform.ragops.metricsreporting.domain.UpdateThresholdCommand
import java.time.Instant

class UpdateAnomalyThresholdsService(
    private val saveAnomalyThresholdPort: SaveAnomalyThresholdPort,
) : UpdateAnomalyThresholdsUseCase {
    override fun update(commands: List<UpdateThresholdCommand>): List<AnomalyThreshold> =
        commands.map { cmd ->
            saveAnomalyThresholdPort.save(
                AnomalyThreshold(
                    metricKey = cmd.metricKey,
                    warnValue = cmd.warnValue,
                    criticalValue = cmd.criticalValue,
                    updatedAt = Instant.now(),
                )
            )
        }
}
