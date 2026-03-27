package com.publicplatform.ragops.adminapi.ingestion

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class IngestionApiTests : BaseApiTest() {

    @Test
    fun `crawl sources returns all organizations for ops admin`() {
        mockMvc.get("/admin/crawl-sources")
            .andExpect {
                status { isOk() }
                jsonPath("$.items[0].id") { value("crawl_src_001") }
                jsonPath("$.items[0].serviceId") { value("svc_welfare") }
                jsonPath("$.items[0].renderMode") { value("browser_playwright") }
                jsonPath("$.items[1].organizationId") { value("org_central_gov") }
            }
    }

    @Test
    fun `crawl sources are scoped to client admin organization`() {
        mockMvc.get("/admin/crawl-sources") {
            header("X-Debug-Role", "client_admin")
            header("X-Debug-Organization-Id", "org_central_gov")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].id") { value("crawl_src_002") }
            jsonPath("$.items[0].organizationId") { value("org_central_gov") }
        }
    }

    @Test
    fun `ingestion jobs are scoped to session organization`() {
        mockMvc.get("/admin/ingestion-jobs") {
            header("X-Debug-Role", "qa_admin")
            header("X-Debug-Organization-Id", "org_local_gov")
        }.andExpect {
            status { isOk() }
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
            content = """
                {
                  "organizationId": "org_central_gov",
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
            header("X-Debug-Organization-Id", "org_central_gov")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_local_gov",
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
        val sessionId = loginAndReturnSessionId(
            email = "client@jcg.com",
            password = "pass1234",
        )

        mockMvc.post("/admin/crawl-sources/crawl_src_002/run") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `qa admin cannot create crawl source without write action`() {
        mockMvc.post("/admin/crawl-sources") {
            header("X-Debug-Role", "qa_admin")
            header("X-Debug-Organization-Id", "org_local_gov")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_local_gov",
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
            content = """{"jobStatus": "running", "jobStage": "fetch"}"""
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
            content = """{"jobStatus": "running", "jobStage": "fetch"}"""
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
                jsonPath("$.organizationId") { value("org_local_gov") }
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
            header("X-Debug-Organization-Id", "org_central_gov")
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
                jsonPath("$.organizationId") { value("org_local_gov") }
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
            header("X-Debug-Organization-Id", "org_central_gov")
        }.andExpect {
            status { isNotFound() }
        }
    }
}
