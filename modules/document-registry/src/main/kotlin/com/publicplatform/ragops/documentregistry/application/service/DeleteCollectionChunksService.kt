package com.publicplatform.ragops.documentregistry.application.service

import com.publicplatform.ragops.documentregistry.application.port.`in`.DeleteCollectionChunksUseCase
import com.publicplatform.ragops.documentregistry.application.port.`in`.DeleteCollectionResult
import com.publicplatform.ragops.documentregistry.application.port.out.DeleteCollectionChunksPort

class DeleteCollectionChunksService(
    private val deleteCollectionChunksPort: DeleteCollectionChunksPort,
) : DeleteCollectionChunksUseCase {

    override fun deleteByCollection(serviceId: String, collectionName: String): DeleteCollectionResult =
        deleteCollectionChunksPort.deleteChunksByCollection(serviceId, collectionName)
}
