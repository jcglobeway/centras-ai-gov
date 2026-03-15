package com.publicplatform.ragops.organizationdirectory

import java.util.concurrent.ConcurrentHashMap

class InMemoryServiceRepository : ServiceRepository {
    private val services = ConcurrentHashMap<String, Service>()

    override fun findByOrganizationId(organizationId: String): List<Service> {
        return services.values.filter { it.organizationId == organizationId }
    }

    override fun findById(id: String): Service? {
        return services[id]
    }

    override fun save(service: Service): Service {
        services[service.id] = service
        return service
    }
}
