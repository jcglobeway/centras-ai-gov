package com.publicplatform.ragops.organizationdirectory.application.port.out

import com.publicplatform.ragops.organizationdirectory.domain.Organization
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationScope
import com.publicplatform.ragops.organizationdirectory.domain.OrganizationSummary

interface LoadOrganizationPort {
    fun getOrganizations(ids: Set<String>): List<OrganizationSummary>
    fun listAll(): List<Organization>
    fun loadByScope(scope: OrganizationScope): List<Organization>
}
