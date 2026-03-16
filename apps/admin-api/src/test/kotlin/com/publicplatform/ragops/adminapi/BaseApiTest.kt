package com.publicplatform.ragops.adminapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
abstract class BaseApiTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    protected fun loginAndReturnSessionId(email: String, password: String): String {
        val response = mockMvc.post("/admin/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email": "$email", "password": "$password"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return response.response.contentAsString.contentAsJson().path("session").path("token").asText()
    }

    protected fun createQuestionAndReturnId(): String {
        val response = mockMvc.post("/admin/questions") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "org_seoul_120",
                  "serviceId": "svc_welfare",
                  "chatSessionId": "chat_session_001",
                  "questionText": "Test question",
                  "channel": "web"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        return response.response.contentAsString.contentAsJson().path("questionId").asText()
    }

    protected fun String.contentAsJson(): JsonNode = objectMapper.readTree(this)
}
