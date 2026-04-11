package com.publicplatform.ragops.documentregistry.application.port.`in`

interface DeleteCollectionChunksUseCase {
    fun deleteByCollection(serviceId: String, collectionName: String): DeleteCollectionResult
}

data class DeleteCollectionResult(val deletedChunks: Int, val resetDocuments: Int)
