package com.publicplatform.ragops.adminapi

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
class AdminApiApplicationTests {
    @Autowired
    lateinit var mockMvc: MockMvc

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
        mockMvc.post("/admin/crawl-sources/crawl_src_001/run")
            .andExpect {
                status { isAccepted() }
            }

        mockMvc.post("/admin/ingestion-jobs/ing_job_901/status") {
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
            jsonPath("$.jobId") { value("ing_job_901") }
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
}
