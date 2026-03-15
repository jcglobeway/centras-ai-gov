package com.publicplatform.ragops.adminapi

import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
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
    fun `crawl sources returns all organizations for ops admin`() {
        mockMvc.get("/admin/crawl-sources")
            .andExpect {
                status { isOk() }
                jsonPath("$.total") { value(2) }
                jsonPath("$.items[0].id") { value("crawl_src_001") }
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
            jsonPath("$.total") { value(1) }
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
            jsonPath("$.items[0].status") { value("succeeded") }
        }
    }
}
