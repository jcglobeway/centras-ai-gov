package com.publicplatform.ragops.adminapi.e2e

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class FullFlowE2ETests : BaseApiTest() {

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
