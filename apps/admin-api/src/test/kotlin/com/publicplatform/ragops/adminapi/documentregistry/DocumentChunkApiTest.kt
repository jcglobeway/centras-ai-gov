package com.publicplatform.ragops.adminapi.documentregistry

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class DocumentChunkApiTest : BaseApiTest() {

    @Test
    fun `document chunk can be saved through admin api`() {
        val sessionId = loginAndReturnSessionId("super@jcg.com", "pass1234")
        val embeddingVector = buildString {
            append("[")
            append(List(1024) { "0.1" }.joinToString(","))
            append("]")
        }

        val documentResponse = mockMvc.post("/admin/documents") {
            header("X-Admin-Session-Id", sessionId)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_local_gov",
                  "title": "Document Chunk Test",
                  "documentType": "webpage",
                  "sourceUri": "https://example.gov.kr/test",
                  "visibilityScope": "organization"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val documentId = documentResponse.response.contentAsString.contentAsJson().path("id").asText()

        mockMvc.post("/admin/document-chunks") {
            header("X-Admin-Session-Id", sessionId)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "documentId": "$documentId",
                  "chunkKey": "chunk_0",
                  "chunkText": "테스트 청크 본문입니다.",
                  "chunkOrder": 0,
                  "tokenCount": 12,
                  "embeddingVector": "$embeddingVector",
                  "metadata": {
                    "sourceUrl": "https://example.gov.kr/test",
                    "pageType": "notice"
                  }
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.documentId") { value(documentId) }
        }
    }
}
