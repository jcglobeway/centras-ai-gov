package com.publicplatform.ragops.identityaccess

import java.util.concurrent.ConcurrentHashMap

class InMemoryAdminUserRepository : AdminUserRepository {
    private val usersById = ConcurrentHashMap<String, AdminUser>()
    private val usersByEmail = ConcurrentHashMap<String, AdminUser>()

    override fun findByEmail(email: String): AdminUser? {
        return usersByEmail[email]
    }

    override fun findById(userId: String): AdminUser? {
        return usersById[userId]
    }

    override fun save(user: AdminUser): AdminUser {
        usersById[user.id] = user
        usersByEmail[user.email] = user
        return user
    }
}
