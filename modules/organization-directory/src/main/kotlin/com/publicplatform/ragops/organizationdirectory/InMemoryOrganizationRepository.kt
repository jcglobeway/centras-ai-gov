package com.publicplatform.ragops.organizationdirectory

import java.util.concurrent.ConcurrentHashMap

class InMemoryOrganizationRepository : OrganizationRepository {
    private val organizations = ConcurrentHashMap<String, Organization>()

    override fun findAll(): List<Organization> {
        return organizations.values.toList()
    }

    override fun findById(id: String): Organization? {
        return organizations[id]
    }

    override fun save(organization: Organization): Organization {
        organizations[organization.id] = organization
        return organization
    }
}
