package com.publicplatform.ragops.adminapi.correction

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class CorrectionApiTest : BaseApiTest() {

    private fun loginAsSuperAdmin() = loginAndReturnSessionId("super@jcg.com", "pass1234")

    @Test
    fun `correction can be created and returns 201 with id`() {
        val sessionId = loginAsSuperAdmin()

        mockMvc.post("/admin/corrections") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Admin-Session-Id", sessionId)
            content = """
                {
                  "questionId": "question_001",
                  "questionText": "복지급여 신청 방법은?",
                  "correctedAnswerText": "복지급여는 읍면동 주민센터에서 신청하실 수 있습니다.",
                  "correctionReason": "기존 답변이 부정확함"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.questionId") { value("question_001") }
        }
    }

    @Test
    fun `correction id is prefixed with correction_`() {
        val sessionId = loginAsSuperAdmin()

        val response = mockMvc.post("/admin/corrections") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Admin-Session-Id", sessionId)
            content = """
                {
                  "questionId": "question_002",
                  "correctedAnswerText": "올바른 답변입니다."
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val id = response.response.contentAsString.contentAsJson().path("id").asText()
        assert(id.startsWith("correction_")) { "ID should start with 'correction_' but was: $id" }
    }

    @Test
    fun `corrections list returns created corrections`() {
        val sessionId = loginAsSuperAdmin()

        mockMvc.post("/admin/corrections") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Admin-Session-Id", sessionId)
            content = """
                {
                  "questionId": "question_list_01",
                  "correctedAnswerText": "목록 조회용 정답입니다."
                }
            """.trimIndent()
        }.andExpect { status { isCreated() } }

        mockMvc.get("/admin/corrections") {
            header("X-Admin-Session-Id", sessionId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items") { isArray() }
            jsonPath("$.total") { isNumber() }
        }
    }
}
