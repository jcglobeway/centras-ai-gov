package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class AuthApiTests : BaseApiTest() {

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
            content = """{"email": "qa@jcg.com", "password": "pass1234"}"""
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
            content = """{"email": "qa@jcg.com", "password": "wrong-password"}"""
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
        val sessionId = loginAndReturnSessionId(
            email = "client@jcg.com",
            password = "pass1234",
        )

        mockMvc.get("/admin/auth/me") {
            header("X-Admin-Session-Id", sessionId)
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
            email = "ops@jcg.com",
            password = "pass1234",
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
}
