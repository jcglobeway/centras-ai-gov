package com.publicplatform.ragops.identityaccess

open class AdminUserRepositoryAdapter(
    private val jpaRepository: JpaAdminUserRepository,
) : AdminUserRepository {

    override fun findByEmail(email: String): AdminUser? {
        return jpaRepository.findByEmail(email)?.toModel()
    }

    override fun findById(userId: String): AdminUser? {
        return jpaRepository.findById(userId).orElse(null)?.toModel()
    }

    override fun save(user: AdminUser): AdminUser {
        val entity = user.toEntity()
        val saved = jpaRepository.save(entity)
        return saved.toModel()
    }
}
