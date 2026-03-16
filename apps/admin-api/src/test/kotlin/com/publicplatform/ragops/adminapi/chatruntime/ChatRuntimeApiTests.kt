package com.publicplatform.ragops.adminapi.chatruntime

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class ChatRuntimeApiTests : BaseApiTest() {

    @Test
    fun `question can be created and retrieved`() {
        createQuestionAndReturnId()

        mockMvc.get("/admin/questions")
            .andExpect {
                status { isOk() }
                jsonPath("$.items") { isArray() }
            }
    }

    @Test
    fun `unresolved questions shows fallback and no_answer cases`() {
        val questionId = createQuestionAndReturnId()

        mockMvc.post("/admin/answers") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "$questionId",
                  "answerText": "Fallback response",
                  "answerStatus": "fallback",
                  "fallbackReasonCode": "LOW_CONFIDENCE"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }

        val unresolvedResponse = mockMvc.get("/admin/questions/unresolved")
            .andExpect {
                status { isOk() }
                jsonPath("$.items") { isArray() }
            }
            .andReturn()

        val unresolvedTotal = unresolvedResponse.response.contentAsString.contentAsJson().path("total").asInt()
        assert(unresolvedTotal >= 1) { "Expected at least 1 unresolved question, but got: $unresolvedTotal" }
    }

    @Test
    fun `documents can be listed with organization scope`() {
        val response = mockMvc.get("/admin/documents")
            .andExpect {
                status { isOk() }
                jsonPath("$.items") { isArray() }
            }
            .andReturn()

        val total = response.response.contentAsString.contentAsJson().path("total").asInt()
        assert(total >= 2) { "Expected at least 2 documents, but got: $total" }
    }

    @Test
    fun `document versions can be listed by document id`() {
        mockMvc.get("/admin/documents/doc_301/versions")
            .andExpect {
                status { isOk() }
                jsonPath("$.items") { isArray() }
                jsonPath("$.items[0].documentId") { value("doc_301") }
            }
    }

    @Test
    fun `daily metrics can be listed with date range`() {
        val response = mockMvc.get("/admin/metrics/daily")
            .andExpect {
                status { isOk() }
                jsonPath("$.items") { isArray() }
            }
            .andReturn()

        val total = response.response.contentAsString.contentAsJson().path("total").asInt()
        assert(total >= 2) { "Expected at least 2 metrics, but got: $total" }
    }
}
