package com.publicplatform.ragops.adminapi.evaluation

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

class RagasEvaluationApiTest : BaseApiTest() {

    @Test
    fun `ragas evaluation can be recorded with full metrics`() {
        mockMvc.post("/admin/ragas-evaluations") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "question_001",
                  "faithfulness": 0.85,
                  "answerRelevancy": 0.91,
                  "contextPrecision": 0.78,
                  "contextRecall": 0.82,
                  "judgeProvider": "ollama",
                  "judgeModel": "qwen2.5:7b"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.questionId") { value("question_001") }
            jsonPath("$.faithfulness") { value(0.85) }
            jsonPath("$.answerRelevancy") { value(0.91) }
            jsonPath("$.judgeProvider") { value("ollama") }
            jsonPath("$.judgeModel") { value("qwen2.5:7b") }
        }
    }

    @Test
    fun `ragas evaluation can be recorded with null metrics`() {
        mockMvc.post("/admin/ragas-evaluations") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "question_002",
                  "faithfulness": null,
                  "answerRelevancy": null,
                  "contextPrecision": null,
                  "contextRecall": null,
                  "judgeProvider": "ollama",
                  "judgeModel": "qwen2.5:7b"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.questionId") { value("question_002") }
            jsonPath("$.faithfulness") { value(null) }
        }
    }

    @Test
    fun `ragas evaluation id is prefixed with ragas_`() {
        val response = mockMvc.post("/admin/ragas-evaluations") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "question_003",
                  "faithfulness": 0.72
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val id = response.response.contentAsString.contentAsJson().path("id").asText()
        assert(id.startsWith("ragas_")) { "ID should start with 'ragas_' but was: $id" }
    }

    @Test
    fun `GET by-question returns 200 when evaluation exists`() {
        mockMvc.post("/admin/ragas-evaluations") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"questionId": "question_get_01", "faithfulness": 0.80}"""
        }.andExpect { status { isCreated() } }

        mockMvc.get("/admin/ragas-evaluations/by-question/question_get_01")
            .andExpect {
                status { isOk() }
                jsonPath("$.questionId") { value("question_get_01") }
                jsonPath("$.faithfulness") { value(0.80) }
            }
    }

    @Test
    fun `GET by-question returns 404 when evaluation does not exist`() {
        mockMvc.get("/admin/ragas-evaluations/by-question/question_nonexistent_xyz")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `PATCH by-question fills null fields and preserves existing values`() {
        mockMvc.post("/admin/ragas-evaluations") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "questionId": "question_patch_01",
                  "faithfulness": 0.90,
                  "answerRelevancy": null,
                  "contextPrecision": null
                }
            """.trimIndent()
        }.andExpect { status { isCreated() } }

        mockMvc.patch("/admin/ragas-evaluations/by-question/question_patch_01") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"answerRelevancy": 0.75, "contextPrecision": 0.65}"""
        }.andExpect { status { isOk() } }

        mockMvc.get("/admin/ragas-evaluations/by-question/question_patch_01")
            .andExpect {
                status { isOk() }
                jsonPath("$.faithfulness") { value(0.90) }
                jsonPath("$.answerRelevancy") { value(0.75) }
                jsonPath("$.contextPrecision") { value(0.65) }
            }
    }

    @Test
    fun `PATCH by-question returns 404 when evaluation does not exist`() {
        mockMvc.patch("/admin/ragas-evaluations/by-question/question_nonexistent_patch") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"faithfulness": 0.5}"""
        }.andExpect { status { isNotFound() } }
    }
}
