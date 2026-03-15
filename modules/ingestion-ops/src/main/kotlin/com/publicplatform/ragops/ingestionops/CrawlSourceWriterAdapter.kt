package com.publicplatform.ragops.ingestionops

import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

open class CrawlSourceWriterAdapter(
    private val jpaRepository: JpaCrawlSourceRepository,
) : CrawlSourceWriter {

    @Transactional
    override fun createSource(command: CreateCrawlSourceCommand): CrawlSourceSummary {
        val id = "crawl_src_${UUID.randomUUID().toString().substring(0, 8)}"
        val entity = CrawlSourceEntity(
            id = id,
            organizationId = command.organizationId,
            serviceId = command.serviceId,
            name = command.name,
            sourceType = command.sourceType.name.lowercase(),
            sourceUri = command.sourceUri,
            collectionMode = command.collectionMode.name.lowercase(),
            renderMode = command.renderMode.name.lowercase(),
            scheduleExpr = command.schedule,
            isActive = true,
            status = CrawlSourceStatus.ACTIVE.name.lowercase(),
            lastCrawledAt = null,
            lastSucceededAt = null,
            lastJobId = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        val saved = jpaRepository.save(entity)
        return saved.toSummary()
    }
}
