package com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.out.LoadRagConfigPort
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfigVersion

open class LoadRagConfigPortAdapter(
    private val jpaRagConfigRepository: JpaRagConfigRepository,
    private val jpaRagConfigVersionRepository: JpaRagConfigVersionRepository,
) : LoadRagConfigPort {

    override fun loadByOrganizationId(organizationId: String): RagConfig? =
        jpaRagConfigRepository.findByOrganizationId(organizationId)?.toModel()

    override fun loadVersions(organizationId: String): List<RagConfigVersion> =
        jpaRagConfigVersionRepository.findByOrganizationIdOrderByVersionDesc(organizationId).map { it.toModel() }

    override fun loadVersion(organizationId: String, version: Int): RagConfigVersion? =
        jpaRagConfigVersionRepository.findByOrganizationIdAndVersion(organizationId, version)?.toModel()
}
