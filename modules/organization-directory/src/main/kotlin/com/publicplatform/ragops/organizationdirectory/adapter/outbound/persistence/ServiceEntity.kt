package com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.domain.Service
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "services")
class ServiceEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "organization_id", nullable = false)
    val organizationId: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "channel_type", nullable = false)
    val channelType: String,

    @Column(name = "status", nullable = false)
    val status: String,

    @Column(name = "go_live_at")
    val goLiveAt: Instant?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)

fun ServiceEntity.toModel(): Service =
    Service(
        id = id, organizationId = organizationId, name = name,
        channelType = channelType, status = status, goLiveAt = goLiveAt, createdAt = createdAt,
    )

fun Service.toEntity(): ServiceEntity =
    ServiceEntity(
        id = id, organizationId = organizationId, name = name,
        channelType = channelType, status = status, goLiveAt = goLiveAt,
        createdAt = createdAt, updatedAt = Instant.now(),
    )
