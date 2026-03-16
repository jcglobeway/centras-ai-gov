package com.publicplatform.ragops.ingestionops.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaCrawlSourceRepository : JpaRepository<CrawlSourceEntity, String>
