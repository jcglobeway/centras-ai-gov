/**
 * AdminUser DB 테이블과 1:1 매핑되는 JPA 엔티티.
 *
 * 도메인 모델과 분리되어 있으므로 비즈니스 로직을 포함하지 않는다.
 * Adapter의 toSummary()/toDomain() 메서드에서 도메인 모델로 변환된다.
 */
package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.publicplatform.ragops.identityaccess.domain.AdminUser
import com.publicplatform.ragops.identityaccess.domain.AdminUserStatus
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
