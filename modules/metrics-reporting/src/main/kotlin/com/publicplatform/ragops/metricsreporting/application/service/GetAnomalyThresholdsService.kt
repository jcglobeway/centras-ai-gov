package com.publicplatform.ragops.metricsreporting.application.service

import com.publicplatform.ragops.metricsreporting.application.port.`in`.GetAnomalyThresholdsUseCase
import com.publicplatform.ragops.metricsreporting.application.port.out.LoadAnomalyThresholdPort
import com.publicplatform.ragops.metricsreporting.domain.AnomalyThreshold

class GetAnomalyThresholdsService(
    private val loadAnomalyThresholdPort: LoadAnomalyThresholdPort,
) : GetAnomalyThresholdsUseCase {
    override fun getAll(): List<AnomalyThreshold> = loadAnomalyThresholdPort.findAll()
}
