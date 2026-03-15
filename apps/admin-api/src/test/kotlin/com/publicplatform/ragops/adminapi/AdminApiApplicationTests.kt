package com.publicplatform.ragops.adminapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@org.springframework.test.context.ActiveProfiles("test")
class AdminApiApplicationTests {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `health endpoint returns ok`() {
        mockMvc.get("/healthz")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("ok") }
                jsonPath("$.service") { value("admin-api") }
            }
    }

    @Test
    fun `auth me returns default development session`() {
        mockMvc.get("/admin/auth/me")
            .andExpect {
                status { isOk() }
                jsonPath("$.user.id") { value("usr_dev_ops_001") }
                jsonPath("$.roles[0].roleCode") { value("ops_admin") }
                jsonPath("$.roles[0].organizationId") { value(nullValue()) }
                jsonPath("$.actions[0]") { value("dashboard.read") }
            }
    }

    @Test
    fun `auth me supports role override headers`() {
        mockMvc.get("/admin/auth/me") {
            header("X-Debug-Role", "qa_admin")
            header("X-Debug-Organization-Id", "org_busan_220")
            header("X-Debug-User-Id", "usr_qa_001")
        }.andExpect {
            status { isOk() }
            jsonPath("$.user.id") { value("usr_qa_001") }
            jsonPath("$.roles[0].roleCode") { value("qa_admin") }
            jsonPath("$.roles[0].organizationId") { value("org_busan_220") }
            jsonPath("$.actions") { isArray() }
        }
    }

    @Test
    fun `login returns session token and authorization summary`() {
        mockMvc.post("/admin/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "email": "qa.manager@gov-platform.kr",
                  "password": "qa-pass-1234"
                }
                """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.user.id") { value("usr_qa_001") }
            jsonPath("$.authorization.primaryRole") { value("qa_admin") }
            jsonPath("$.authorization.organizationScope[0]") { value("org_seoul_120") }
            jsonPath("$.session.token") { exists() }
        }
    }

    @Test
    fun `login rejects invalid credentials`() {
        mockMvc.post("/admin/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "email": "qa.manager@gov-platform.kr",
                  "password": "wrong-password"
                }
                """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("AUTH_INVALID_CREDENTIALS") }
        }
    }

    @Test
    fun `auth me clears unknown organization scope`() {
        mockMvc.get("/admin/auth/me") {
            header("X-Debug-Role", "client_admin")
            header("X-Debug-Organization-Id", "org_unknown_999")
        }.andExpect {
            status { isOk() }
            jsonPath("$.roles[0].roleCode") { value("client_admin") }
            jsonPath("$.roles[0].organizationId") { value(nullValue()) }
        }
    }

    @Test
    fun `auth me restores session from session id header`() {
        mockMvc.get("/admin/auth/me") {
            header("X-Admin-Session-Id", "sess_client_busan_001")
        }.andExpect {
            status { isOk() }
            jsonPath("$.user.id") { value("usr_client_busan_001") }
            jsonPath("$.roles[0].roleCode") { value("client_admin") }
            jsonPath("$.roles[0].organizationId") { value("org_busan_220") }
            jsonPath("$.actions") { isArray() }
        }
    }

    @Test
    fun `auth me rejects expired session id instead of falling back to debug session`() {
        mockMvc.get("/admin/auth/me") {
            header("X-Admin-Session-Id", "sess_expired_qa_001")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("AUTH_SESSION_EXPIRED") }
        }
    }

    @Test
    fun `logout revokes issued session and blocks later restore`() {
        val sessionId = loginAndReturnSessionId(
            email = "ops.platform@gov-platform.kr",
            password = "ops-pass-1234",
        )

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
    fun `crawl sources returns all organizations for ops admin`() {
        mockMvc.get("/admin/crawl-sources")
            .andExpect {
                status { isOk() }
                jsonPath("$.items[0].id") { value("crawl_src_001") }
                jsonPath("$.items[0].serviceId") { value("svc_welfare") }
                jsonPath("$.items[0].renderMode") { value("browser_playwright") }
                jsonPath("$.items[1].organizationId") { value("org_busan_220") }
            }
    }

    @Test
    fun `crawl sources are scoped to client admin organization`() {
        mockMvc.get("/admin/crawl-sources") {
            header("X-Debug-Role", "client_admin")
            header("X-Debug-Organization-Id", "org_busan_220")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].id") { value("crawl_src_002") }
            jsonPath("$.items[0].organizationId") { value("org_busan_220") }
        }
    }

    @Test
    fun `ingestion jobs are scoped to session organization`() {
        mockMvc.get("/admin/ingestion-jobs") {
            header("X-Debug-Role", "qa_admin")
            header("X-Debug-Organization-Id", "org_seoul_120")
        }.andExpect {
            status { isOk() }
            jsonPath("$.total") { value(1) }
            jsonPath("$.items[0].id") { value("ing_job_101") }
            jsonPath("$.items[0].jobType") { value("crawl") }
            jsonPath("$.items[0].jobStage") { value("complete") }
            jsonPath("$.items[0].status") { value("succeeded") }
        }
    }

    @Test
    fun `ops admin can create crawl source`() {
        mockMvc.post("/admin/crawl-sources") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "organizationId": "org_busan_220",
                  "serviceId": "svc_faq",
                  "name": "Busan Welfare Notices",
                  "sourceType": "website",
                  "sourceUri": "https://busan.example.go.kr/welfare",
                  "renderMode": "browser_playwright",
                  "collectionMode": "incremental",
                  "scheduleExpr": "0 */4 * * *"
                }
                """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.saved") { value(true) }
            jsonPath("$.crawlSourceId") { exists() }
        }
    }

    @Test
    fun `client admin cannot create crawl source outside own scope`() {
        mockMvc.post("/admin/crawl-sources") {
            header("X-Debug-Role", "client_admin")
            header("X-Debug-Organization-Id", "org_busan_220")
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "organizationId": "org_seoul_120",
                  "serviceId": "svc_welfare",
                  "name": "Forbidden Source",
                  "sourceType": "website",
                  "sourceUri": "https://seoul.example.go.kr/forbidden",
                  "renderMode": "browser_playwright",
                  "collectionMode": "incremental",
                  "scheduleExpr": "0 */4 * * *"
                }
                """.trimIndent()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `client admin cannot run crawl source without write action`() {
        mockMvc.post("/admin/crawl-sources/crawl_src_002/run") {
            header("X-Admin-Session-Id", "sess_client_busan_001")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `qa admin cannot create crawl source without write action`() {
        mockMvc.post("/admin/crawl-sources") {
            header("X-Debug-Role", "qa_admin")
            header("X-Debug-Organization-Id", "org_seoul_120")
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "organizationId": "org_seoul_120",
                  "serviceId": "svc_welfare",
                  "name": "QA Forbidden Source",
                  "sourceType": "website",
                  "sourceUri": "https://seoul.example.go.kr/qa-forbidden",
                  "renderMode": "browser_playwright",
                  "collectionMode": "incremental",
                  "scheduleExpr": "0 */4 * * *"
                }
                """.trimIndent()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `run crawl source creates queued ingestion job`() {
        mockMvc.post("/admin/crawl-sources/crawl_src_001/run")
            .andExpect {
                status { isAccepted() }
                jsonPath("$.jobId") { exists() }
                jsonPath("$.status") { value("queued") }
            }
    }

    @Test
    fun `ingestion job transition accepts queued to running`() {
        val response = mockMvc.post("/admin/crawl-sources/crawl_src_001/run")
            .andExpect {
                status { isAccepted() }
                jsonPath("$.jobId") { exists() }
            }
            .andReturn()

        val jobId = response.response.contentAsString.contentAsJson().path("jobId").asText()

        mockMvc.post("/admin/ingestion-jobs/$jobId/status") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "jobStatus": "running",
                  "jobStage": "fetch"
                }
                """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.jobId") { value(jobId) }
            jsonPath("$.jobStatus") { value("running") }
            jsonPath("$.jobStage") { value("fetch") }
        }
    }

    @Test
    fun `ingestion job transition rejects terminal to running`() {
        mockMvc.post("/admin/ingestion-jobs/ing_job_101/status") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "jobStatus": "running",
                  "jobStage": "fetch"
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `get crawl source by id returns source details`() {
        mockMvc.get("/admin/crawl-sources/crawl_src_001")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value("crawl_src_001") }
                jsonPath("$.organizationId") { value("org_seoul_120") }
                jsonPath("$.serviceId") { value("svc_welfare") }
                jsonPath("$.name") { value("Seoul Notices") }
                jsonPath("$.sourceType") { value("website") }
                jsonPath("$.sourceUri") { value("https://seoul.example.go.kr/notices") }
                jsonPath("$.renderMode") { value("browser_playwright") }
                jsonPath("$.collectionMode") { value("incremental") }
            }
    }

    @Test
    fun `get crawl source by id returns 404 for unknown source`() {
        mockMvc.get("/admin/crawl-sources/crawl_src_unknown_999")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `get crawl source by id respects organization scope`() {
        mockMvc.get("/admin/crawl-sources/crawl_src_001") {
            header("X-Debug-Role", "client_admin")
            header("X-Debug-Organization-Id", "org_busan_220")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `get ingestion job by id returns job details`() {
        mockMvc.get("/admin/ingestion-jobs/ing_job_101")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value("ing_job_101") }
                jsonPath("$.organizationId") { value("org_seoul_120") }
                jsonPath("$.crawlSourceId") { value("crawl_src_001") }
                jsonPath("$.jobType") { value("crawl") }
                jsonPath("$.jobStage") { value("complete") }
                jsonPath("$.status") { value("succeeded") }
            }
    }

    @Test
    fun `get ingestion job by id returns 404 for unknown job`() {
        mockMvc.get("/admin/ingestion-jobs/ing_job_unknown_999")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `get ingestion job by id respects organization scope`() {
        mockMvc.get("/admin/ingestion-jobs/ing_job_101") {
            header("X-Debug-Role", "qa_admin")
            header("X-Debug-Organization-Id", "org_busan_220")
        }.andExpect {
            status { isNotFound() }
        }
    }

    private fun loginAndReturnSessionId(email: String, password: String): String {
        val response = mockMvc.post("/admin/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "email": "$email",
                  "password": "$password"
                }
                """.trimIndent()
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return response.response.contentAsString.contentAsJson().path("session").path("token").asText()
    }

    private fun String.contentAsJson(): JsonNode =
        objectMapper.readTree(this)
}
