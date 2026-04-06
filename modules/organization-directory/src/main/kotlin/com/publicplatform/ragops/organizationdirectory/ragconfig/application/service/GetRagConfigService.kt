package com.publicplatform.ragops.organizationdirectory.ragconfig.application.service

import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`.GetRagConfigUseCase
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.out.LoadRagConfigPort
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfigVersion

open class GetRagConfigService(
    private val loadRagConfigPort: LoadRagConfigPort,
) : GetRagConfigUseCase {

    override fun getRagConfig(organizationId: String): RagConfig? =
        loadRagConfigPort.loadByOrganizationId(organizationId)

    override fun listVersions(organizationId: String): List<RagConfigVersion> =
        loadRagConfigPort.loadVersions(organizationId)
}
