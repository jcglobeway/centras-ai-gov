package com.publicplatform.ragops.adminapi.documentregistry.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.documentregistry.application.port.`in`.RegisterDocumentUseCase
import com.publicplatform.ragops.documentregistry.domain.RegisterDocumentCommand
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationPolicy
import com.publicplatform.ragops.identityaccess.domain.AuthorizationCheck
import com.publicplatform.ragops.ingestionops.application.port.`in`.CreateCrawlSourceUseCase
import com.publicplatform.ragops.ingestionops.application.port.`in`.RunIngestionJobUseCase
import com.publicplatform.ragops.ingestionops.domain.CrawlCollectionMode
import com.publicplatform.ragops.ingestionops.domain.CrawlRenderMode
import com.publicplatform.ragops.ingestionops.domain.CrawlSourceType
import com.publicplatform.ragops.ingestionops.domain.CreateCrawlSourceCommand
import com.publicplatform.ragops.ingestionops.domain.IngestionJobType
import com.publicplatform.ragops.ingestionops.domain.RequestIngestionJobCommand
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

@RestController
@RequestMapping("/admin")
class DocumentUploadController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val adminAuthorizationPolicy: AdminAuthorizationPolicy,
    private val registerDocumentUseCase: RegisterDocumentUseCase,
    private val createCrawlSourceUseCase: CreateCrawlSourceUseCase,
    private val runIngestionJobUseCase: RunIngestionJobUseCase,
    @Value("\${app.upload.dir:}") private val uploadDirConfig: String,
) {

    private val uploadDir: String get() =
        uploadDirConfig.ifBlank { System.getProperty("java.io.tmpdir") + "/ragops-uploads" }

    @PostMapping("/documents/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadDocument(
        @RequestParam("files") files: List<MultipartFile>,
        @RequestParam("organizationId") organizationId: String,
        @RequestParam("serviceId") serviceId: String,
        @RequestParam("collectionName", required = false) collectionName: String?,
        servletRequest: HttpServletRequest,
    ): List<DocumentUploadResponse> {
        val session = adminRequestSessionResolver.resolve(servletRequest)
        try {
            adminAuthorizationPolicy.requireAuthorized(session, AuthorizationCheck("crawl_source.write", organizationId))
        } catch (e: com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationException) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, e.message, e)
        }

        return files.map { file ->
            val originalFilename = file.originalFilename?.takeIf { it.isNotBlank() }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 이름이 없습니다.")

            val savedPath = saveFileToDisk(file, organizationId, originalFilename)

            val crawlSource = createCrawlSourceUseCase.execute(
                CreateCrawlSourceCommand(
                    organizationId = organizationId,
                    serviceId = serviceId,
                    name = "파일업로드: $originalFilename",
                    sourceType = CrawlSourceType.FILE_DROP,
                    sourceUri = savedPath,
                    renderMode = CrawlRenderMode.HTTP_STATIC,
                    collectionMode = CrawlCollectionMode.FULL,
                    schedule = "manual",
                    requestedBy = session.user.id,
                    collectionName = collectionName,
                ),
            )

            val documentType = originalFilename.substringAfterLast('.', "bin").lowercase()
            val document = registerDocumentUseCase.execute(
                RegisterDocumentCommand(
                    organizationId = organizationId,
                    title = originalFilename,
                    documentType = documentType,
                    sourceUri = savedPath,
                    visibilityScope = "organization",
                    requestedBy = session.user.id,
                    collectionName = collectionName,
                    crawlSourceId = crawlSource.id,
                ),
            )

            val job = runIngestionJobUseCase.execute(
                RequestIngestionJobCommand(
                    crawlSourceId = crawlSource.id,
                    requestedBy = session.user.id,
                    triggerType = "file_upload",
                    jobType = IngestionJobType.CRAWL,
                    requestedAt = Instant.now(),
                ),
            )

            DocumentUploadResponse(
                documentId = document.id,
                crawlSourceId = crawlSource.id,
                jobId = job.id,
                status = job.status.name.lowercase(),
            )
        }
    }

    private fun saveFileToDisk(file: MultipartFile, organizationId: String, filename: String): String {
        val orgDir = Paths.get(uploadDir, organizationId)
        Files.createDirectories(orgDir)
        val timestamp = Instant.now().toEpochMilli()
        val safeName = filename.replace(Regex("[^a-zA-Z0-9._\\-가-힣]"), "_")
        val target = orgDir.resolve("${timestamp}_$safeName")
        file.transferTo(target.toFile())
        return target.toAbsolutePath().toString()
    }
}

data class DocumentUploadResponse(
    val documentId: String,
    val crawlSourceId: String,
    val jobId: String,
    val status: String,
)
