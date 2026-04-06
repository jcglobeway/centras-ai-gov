package com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence

import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.out.RecordRagConfigPort
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfig
import com.publicplatform.ragops.organizationdirectory.ragconfig.domain.RagConfigVersion

open class RecordRagConfigPortAdapter(
    private val jpaRagConfigRepository: JpaRagConfigRepository,
    private val jpaRagConfigVersionRepository: JpaRagConfigVersionRepository,
) : RecordRagConfigPort {

    override fun save(config: RagConfig): RagConfig =
        jpaRagConfigRepository.save(config.toEntity()).toModel()

    override fun saveVersion(version: RagConfigVersion): RagConfigVersion =
        jpaRagConfigVersionRepository.save(version.toEntity()).toModel()
}
