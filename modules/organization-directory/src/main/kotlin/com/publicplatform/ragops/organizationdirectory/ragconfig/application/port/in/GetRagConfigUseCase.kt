package com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.`in`

import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfigVersion

interface GetRagConfigUseCase {
    fun getRagConfig(organizationId: String): RagConfig?
    fun listVersions(organizationId: String): List<RagConfigVersion>
}
