package com.publicplatform.ragops.identityaccess

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "admin_users")
class AdminUserEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "display_name", nullable = false)
    val displayName: String,

    @Column(name = "status", nullable = false)
    val status: String,

    @Column(name = "last_login_at")
    val lastLoginAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

fun AdminUserEntity.toModel(): AdminUser =
    AdminUser(
        id = id,
        email = email,
        displayName = displayName,
        status = AdminUserStatus.valueOf(status.uppercase()),
        lastLoginAt = lastLoginAt,
    )

fun AdminUser.toEntity(): AdminUserEntity =
    AdminUserEntity(
        id = id,
        email = email,
        displayName = displayName,
        status = status.name.lowercase(),
        lastLoginAt = lastLoginAt,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
