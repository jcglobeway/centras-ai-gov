package com.publicplatform.ragops.adminapi.qareview

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class QAReviewApiTests : BaseApiTest() {

    @Test
    fun `qa review can be created with confirmed_issue status`() {
        val questionId = createQuestionAndReturnId()

        mockMvc.post("/admin/qa-reviews") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "$questionId",
                  "reviewStatus": "confirmed_issue",
                  "rootCauseCode": "missing_document",
                  "actionType": "document_fix_request",
                  "reviewComment": "Seoul welfare document is missing"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.qaReviewId") { exists() }
            jsonPath("$.questionId") { value(questionId) }
            jsonPath("$.reviewStatus") { value("confirmed_issue") }
        }
    }

    @Test
    fun `qa review rejects confirmed_issue without root_cause`() {
        val questionId = createQuestionAndReturnId()

        mockMvc.post("/admin/qa-reviews") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "$questionId",
                  "reviewStatus": "confirmed_issue",
                  "actionType": "ops_issue"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `qa review enforces false_alarm action_type constraint`() {
        val questionId = createQuestionAndReturnId()

        mockMvc.post("/admin/qa-reviews") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "$questionId",
                  "reviewStatus": "false_alarm",
                  "actionType": "document_fix_request"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `qa review list can be filtered by question_id`() {
        val questionId = createQuestionAndReturnId()

        mockMvc.post("/admin/qa-reviews") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "$questionId",
                  "reviewStatus": "pending",
                  "reviewComment": "Need more review"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }

        mockMvc.get("/admin/qa-reviews?questionId=$questionId")
            .andExpect {
                status { isOk() }
                jsonPath("$.total") { value(1) }
                jsonPath("$.items[0].questionId") { value(questionId) }
                jsonPath("$.items[0].reviewStatus") { value("pending") }
            }
    }

    @Test
    fun `qa admin can create reviews but client admin cannot`() {
        val questionId1 = createQuestionAndReturnId()
        val questionId2 = createQuestionAndReturnId()

        val qaSessionId = loginAndReturnSessionId(
            email = "qa@jcg.com",
            password = "pass1234",
        )

        mockMvc.post("/admin/qa-reviews") {
            header("X-Admin-Session-Id", qaSessionId)
            contentType = MediaType.APPLICATION_JSON
            content = """{"questionId": "$questionId1", "reviewStatus": "pending"}"""
        }.andExpect {
            status { isCreated() }
        }

        val clientSessionId = loginAndReturnSessionId(
            email = "client@jcg.com",
            password = "pass1234",
        )

        mockMvc.post("/admin/qa-reviews") {
            header("X-Admin-Session-Id", clientSessionId)
            contentType = MediaType.APPLICATION_JSON
            content = """{"questionId": "$questionId2", "reviewStatus": "pending"}"""
        }.andExpect {
            status { isForbidden() }
        }
    }
}
