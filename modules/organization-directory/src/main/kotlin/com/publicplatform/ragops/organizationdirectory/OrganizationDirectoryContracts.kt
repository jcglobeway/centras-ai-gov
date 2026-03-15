package com.publicplatform.ragops.organizationdirectory

data class OrganizationSummary(
    val id: String,
    val name: String,
    val institutionType: String,
)

interface OrganizationDirectoryReader {
    fun getOrganizations(ids: Set<String>): List<OrganizationSummary>
}
