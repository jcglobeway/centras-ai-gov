package com.publicplatform.ragops.ingestionops

open class CrawlSourceReaderAdapter(
    private val jpaRepository: JpaCrawlSourceRepository,
) : CrawlSourceReader {

    override fun listSources(scope: IngestionScope): List<CrawlSourceSummary> {
        val allSources = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) {
            allSources
        } else {
            allSources.filter { it.organizationId in scope.organizationIds }
        }
    }
}
