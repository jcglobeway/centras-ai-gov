package com.publicplatform.ragops.ingestionops

open class IngestionJobReaderAdapter(
    private val jpaRepository: JpaIngestionJobRepository,
) : IngestionJobReader {

    override fun listJobs(scope: IngestionScope): List<IngestionJobSummary> {
        val allJobs = jpaRepository.findAll().map { it.toSummary() }
        return if (scope.globalAccess) {
            allJobs
        } else {
            allJobs.filter { it.organizationId in scope.organizationIds }
        }
    }
}
