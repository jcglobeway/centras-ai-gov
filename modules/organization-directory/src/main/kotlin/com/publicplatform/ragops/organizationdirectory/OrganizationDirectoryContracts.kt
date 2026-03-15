package com.publicplatform.ragops.organizationdirectory

import java.time.Instant

data class OrganizationSummary(
    val id: String,
    val name: String,
    val institutionType: String,
)

data class Organization(
    val id: String,
    val name: String,
    val orgCode: String,
    val status: String,
    val institutionType: String,
    val ownerUserId: String?,
    val lastDocumentSyncAt: Instant?,
    val createdAt: Instant,
)

data class Service(
    val id: String,
    val organizationId: String,
    val name: String,
    val channelType: String,
    val status: String,
    val goLiveAt: Instant?,
    val createdAt: Instant,
)

interface OrganizationDirectoryReader {
    fun getOrganizations(ids: Set<String>): List<OrganizationSummary>
}

interface OrganizationRepository {
    fun findAll(): List<Organization>
    fun findById(id: String): Organization?
    fun save(organization: Organization): Organization
}

interface ServiceRepository {
    fun findByOrganizationId(organizationId: String): List<Service>
    fun findById(id: String): Service?
    fun save(service: Service): Service
}
