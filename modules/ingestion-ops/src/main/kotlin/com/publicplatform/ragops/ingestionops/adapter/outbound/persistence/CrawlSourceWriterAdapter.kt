package com.publicplatform.ragops.ingestionops.adapter.outbound.persistence

import com.publicplatform.ragops.ingestionops.domain.CrawlSourceStatus
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceSummary
import com.publicplatform.ragops.ingestionops.domain.CreateCrawlSourceCommand
import com.publicplatform.ragops.ingestionops.application.port.out.SaveCrawlSourcePort
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class SaveCrawlSourcePortAdapter(
    private val jpaRepository: JpaCrawlSourceRepository,
) : SaveCrawlSourcePort {

    @Transactional
    override fun createSource(command: CreateCrawlSourceCommand): CrawlSourceSummary {
        val id = "crawl_src_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = CrawlSourceEntity(
            id = id, organizationId = command.organizationId, serviceId = command.serviceId,
            name = command.name, sourceType = command.sourceType.name.lowercase(),
            sourceUri = command.sourceUri, collectionMode = command.collectionMode.name.lowercase(),
            renderMode = command.renderMode.name.lowercase(), scheduleExpr = command.schedule,
            isActive = true, status = CrawlSourceStatus.ACTIVE.name.lowercase(),
            lastCrawledAt = null, lastSucceededAt = null, lastJobId = null,
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
