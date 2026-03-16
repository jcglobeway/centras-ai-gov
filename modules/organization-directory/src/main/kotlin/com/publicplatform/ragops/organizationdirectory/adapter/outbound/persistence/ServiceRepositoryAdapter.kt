package com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.domain.Service
import com.publicplatform.ragops.organizationdirectory.application.port.out.ServiceRepository

open class ServiceRepositoryAdapter(
    private val jpaRepository: JpaServiceRepository,
) : ServiceRepository {

    override fun findByOrganizationId(organizationId: String): List<Service> =
        jpaRepository.findByOrganizationId(organizationId).map { it.toModel() }

    override fun findById(id: String): Service? = jpaRepository.findById(id).orElse(null)?.toModel()

    override fun save(service: Service): Service {
        val entity = service.toEntity()
        val saved = jpaRepository.save(entity)
        return saved.toModel()
    }
}
