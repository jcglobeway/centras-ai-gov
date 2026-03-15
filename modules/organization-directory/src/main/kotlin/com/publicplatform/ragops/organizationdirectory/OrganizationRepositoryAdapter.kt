package com.publicplatform.ragops.organizationdirectory

open class OrganizationRepositoryAdapter(
    private val jpaRepository: JpaOrganizationRepository,
) : OrganizationRepository {

    override fun findAll(): List<Organization> {
        return jpaRepository.findAll().map { it.toModel() }
    }

    override fun findById(id: String): Organization? {
        return jpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun save(organization: Organization): Organization {
        val entity = organization.toEntity()
        val saved = jpaRepository.save(entity)
        return saved.toModel()
    }
}
