package com.publicplatform.ragops.organizationdirectory

open class ServiceRepositoryAdapter(
    private val jpaRepository: JpaServiceRepository,
) : ServiceRepository {

    override fun findByOrganizationId(organizationId: String): List<Service> {
        return jpaRepository.findByOrganizationId(organizationId).map { it.toModel() }
    }

    override fun findById(id: String): Service? {
        return jpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun save(service: Service): Service {
        val entity = service.toEntity()
        val saved = jpaRepository.save(entity)
        return saved.toModel()
    }
}
