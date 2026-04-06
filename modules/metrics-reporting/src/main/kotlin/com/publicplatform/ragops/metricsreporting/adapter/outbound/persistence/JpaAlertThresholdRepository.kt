package com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaAlertThresholdRepository : JpaRepository<AlertThresholdEntity, String>