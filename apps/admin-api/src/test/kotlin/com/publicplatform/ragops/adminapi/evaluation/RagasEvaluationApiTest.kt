package com.publicplatform.ragops.adminapi.evaluation

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
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
}
