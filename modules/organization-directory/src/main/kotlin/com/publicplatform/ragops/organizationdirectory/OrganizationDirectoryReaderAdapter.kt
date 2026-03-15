package com.publicplatform.ragops.organizationdirectory

open class OrganizationDirectoryReaderAdapter(
    private val jpaRepository: JpaOrganizationRepository,
) : OrganizationDirectoryReader {

    override fun getOrganizations(ids: Set<String>): List<OrganizationSummary> {
        return jpaRepository.findAllById(ids).map { it.toSummary() }
    }
}
