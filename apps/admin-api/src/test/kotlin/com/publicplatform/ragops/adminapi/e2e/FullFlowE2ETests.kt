package com.publicplatform.ragops.adminapi.e2e

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class FullFlowE2ETests : BaseApiTest() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `e2e full auth lifecycle from login to logout and session expiry`() {
        val sessionId = loginAndReturnSessionId(
            email = "ops@jcg.com",
            password = "pass1234",
        )

        mockMvc.get("/admin/auth/me") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.user.email") { value("ops@jcg.com") }
            jsonPath("$.roles[0].roleCode") { value("ops_admin") }
        }

        mockMvc.post("/admin/auth/logout") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.revoked") { value(true) }
        }

        mockMvc.get("/admin/auth/me") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("AUTH_SESSION_REVOKED") }
        }
    }

    @Test
    fun `e2e full ingestion flow from source creation to job completion`() {
        val createResponse = mockMvc.post("/admin/crawl-sources") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_local_gov",
                  "serviceId": "svc_welfare",
                  "name": "E2E Test Source",
                  "sourceType": "website",
                  "sourceUri": "https://example.gov.kr/test",
                  "renderMode": "browser_playwright",
                  "collectionMode": "incremental",
                  "scheduleExpr": "0 */6 * * *"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.saved") { value(true) }
            jsonPath("$.crawlSourceId") { exists() }
        }.andReturn()

        val sourceId = createResponse.response.contentAsString.contentAsJson().path("crawlSourceId").asText()

        val runResponse = mockMvc.post("/admin/crawl-sources/$sourceId/run")
            .andExpect {
                status { isAccepted() }
                jsonPath("$.jobId") { exists() }
                jsonPath("$.status") { value("queued") }
            }.andReturn()

        val jobId = runResponse.response.contentAsString.contentAsJson().path("jobId").asText()

        mockMvc.post("/admin/ingestion-jobs/$jobId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"jobStatus": "running", "jobStage": "fetch"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.jobStatus") { value("running") }
            jsonPath("$.jobStage") { value("fetch") }
        }

        mockMvc.post("/admin/ingestion-jobs/$jobId/status") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"jobStatus": "succeeded", "jobStage": "complete"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.jobStatus") { value("succeeded") }
            jsonPath("$.jobStage") { value("complete") }
        }

        mockMvc.get("/admin/ingestion-jobs/$jobId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(jobId) }
                jsonPath("$.status") { value("succeeded") }
                jsonPath("$.jobStage") { value("complete") }
                jsonPath("$.crawlSourceId") { value(sourceId) }
            }

        mockMvc.get("/admin/crawl-sources/$sourceId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(sourceId) }
                jsonPath("$.status") { value("active") }
                jsonPath("$.lastJobId") { value(jobId) }
            }
    }

    @Test
    fun `e2e delete collection chunks resets indexed documents`() {
        val collectionName = "e2e-delete-collection"

        val sourceResponse = mockMvc.post("/admin/crawl-sources") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_local_gov",
                  "serviceId": "svc_welfare",
                  "name": "E2E Delete Source",
                  "sourceType": "website",
                  "sourceUri": "https://example.gov.kr/delete-test",
                  "renderMode": "http_static",
                  "collectionMode": "full",
                  "scheduleExpr": "0 */6 * * *",
                  "collectionName": "$collectionName"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.crawlSourceId") { exists() }
        }.andReturn()

        val sourceId = sourceResponse.response.contentAsString.contentAsJson().path("crawlSourceId").asText()

        val documentResponse = mockMvc.post("/admin/documents") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_local_gov",
                  "title": "E2E Delete Document",
                  "documentType": "webpage",
                  "sourceUri": "https://example.gov.kr/delete-test/doc",
                  "visibilityScope": "organization",
                  "collectionName": "$collectionName",
                  "crawlSourceId": "$sourceId"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
        }.andReturn()

        val documentId = documentResponse.response.contentAsString.contentAsJson().path("id").asText()

        mockMvc.post("/admin/document-chunks") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "documentId": "$documentId",
                  "chunkKey": "chunk_0",
                  "chunkText": "첫 번째 E2E 청크",
                  "chunkOrder": 0
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }

        mockMvc.post("/admin/document-chunks") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "documentId": "$documentId",
                  "chunkKey": "chunk_1",
                  "chunkText": "두 번째 E2E 청크",
                  "chunkOrder": 1
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }

        val beforeDeleteChunks = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?",
            Int::class.java,
            documentId,
        ) ?: 0
        assert(beforeDeleteChunks == 2) { "expected 2 chunks before delete, but got $beforeDeleteChunks" }

        mockMvc.delete("/admin/collections/chunks") {
            param("serviceId", "svc_welfare")
            param("collectionName", collectionName)
        }.andExpect {
            status { isOk() }
            jsonPath("$.deletedChunks") { value(2) }
            jsonPath("$.resetDocuments") { value(1) }
        }

        val afterDeleteChunks = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?",
            Int::class.java,
            documentId,
        ) ?: 0
        assert(afterDeleteChunks == 0) { "expected 0 chunks after delete, but got $afterDeleteChunks" }

        val statuses = jdbcTemplate.queryForMap(
            "SELECT ingestion_status, index_status FROM documents WHERE id = ?",
            documentId,
        )
        assert(statuses["ingestion_status"] == "pending") {
            "expected ingestion_status=pending, but got ${statuses["ingestion_status"]}"
        }
        assert(statuses["index_status"] == "not_indexed") {
            "expected index_status=not_indexed, but got ${statuses["index_status"]}"
        }
    }

    @Test
    fun `e2e client admin cannot access resources outside organization scope`() {
        val sessionId = loginAndReturnSessionId(
            email = "client@jcg.com",
            password = "pass1234",
        )

        mockMvc.get("/admin/crawl-sources/crawl_src_002") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.organizationId") { value("org_central_gov") }
        }

        mockMvc.get("/admin/crawl-sources/crawl_src_001") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.post("/admin/crawl-sources/crawl_src_001/run") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.post("/admin/crawl-sources") {
            header("X-Admin-Session-Id", sessionId)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_central_gov",
                  "serviceId": "svc_faq",
                  "name": "Forbidden",
                  "sourceType": "website",
                  "sourceUri": "https://test.example.kr",
                  "renderMode": "http_static",
                  "collectionMode": "full",
                  "scheduleExpr": "0 0 * * *"
                }
            """.trimIndent()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `e2e multi-tenant data isolation between organizations`() {
        val opsSessionId = loginAndReturnSessionId(
            email = "ops@jcg.com",
            password = "pass1234",
        )

        val opsResponse = mockMvc.get("/admin/crawl-sources") {
            header("X-Admin-Session-Id", opsSessionId)
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val opsTotal = opsResponse.response.contentAsString.contentAsJson().path("total").asInt()
        assert(opsTotal >= 2) { "ops_admin should see at least 2 sources, but saw: $opsTotal" }

        val clientSessionId = loginAndReturnSessionId(
            email = "client@jcg.com",
            password = "pass1234",
        )

        val clientSources = mockMvc.get("/admin/crawl-sources") {
            header("X-Admin-Session-Id", clientSessionId)
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val items = clientSources.response.contentAsString.contentAsJson().path("items")
        val orgIds = items.map { it.path("organizationId").asText() }.toSet()

        assert(orgIds == setOf("org_central_gov")) {
            "client_admin should only see org_central_gov sources, but saw: $orgIds"
        }
    }
}
