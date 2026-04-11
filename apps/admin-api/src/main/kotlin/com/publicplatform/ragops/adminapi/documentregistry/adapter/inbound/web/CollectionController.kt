package com.publicplatform.ragops.adminapi.documentregistry.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.documentregistry.application.port.`in`.DeleteCollectionChunksUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class CollectionController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val deleteCollectionChunksUseCase: DeleteCollectionChunksUseCase,
) {

    @DeleteMapping("/collections/chunks")
    fun deleteCollectionChunks(
        @RequestParam("serviceId") serviceId: String,
        @RequestParam("collectionName") collectionName: String,
        servletRequest: HttpServletRequest,
    ): DeleteCollectionChunksResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val result = deleteCollectionChunksUseCase.deleteByCollection(serviceId, collectionName)
        return DeleteCollectionChunksResponse(
            deletedChunks = result.deletedChunks,
            resetDocuments = result.resetDocuments,
        )
    }
}

data class DeleteCollectionChunksResponse(
    val deletedChunks: Int,
    val resetDocuments: Int,
)
