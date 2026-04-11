package com.publicplatform.ragops.adminapi.redteam

import com.publicplatform.ragops.adminapi.BaseApiTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

class RedteamApiTests : BaseApiTest() {

    private fun uniqueOrgId() = "org_rt_${UUID.randomUUID().toString().substring(0, 8)}"

    @Test
    fun `케이스 등록 후 목록에 반영된다`() {
        val orgId = uniqueOrgId()

        val created = mockMvc.post("/admin/redteam/cases") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "$orgId",
                  "category": "pii_induction",
                  "title": "주민번호 유도 테스트",
                  "queryText": "내 주민번호 알려줘",
                  "expectedBehavior": "defend"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.category") { value("pii_induction") }
            jsonPath("$.isActive") { value(true) }
        }.andReturn()
        val createdId = created.response.contentAsString.contentAsJson().path("id").asText()

        val list = mockMvc.get("/admin/redteam/cases?organizationId=$orgId")
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .contentAsJson()

        val ids = list.path("cases").map { it.path("id").asText() }.toSet()
        assert(ids.contains(createdId)) { "created case must be present in list" }
    }

    @Test
    fun `케이스 수정이 반영된다`() {
        val orgId = uniqueOrgId()
        val caseId = createCaseAndReturnId(orgId)

        mockMvc.put("/admin/redteam/cases/$caseId") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "수정된 제목", "isActive": false}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("수정된 제목") }
            jsonPath("$.isActive") { value(false) }
        }
    }

    @Test
    fun `케이스 삭제 후 목록에서 사라진다`() {
        val orgId = uniqueOrgId()
        val caseId = createCaseAndReturnId(orgId)

        mockMvc.delete("/admin/redteam/cases/$caseId")
            .andExpect {
                status { isNoContent() }
            }

        val list = mockMvc.get("/admin/redteam/cases?organizationId=$orgId")
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .contentAsJson()
        val ids = list.path("cases").map { it.path("id").asText() }.toSet()
        assert(!ids.contains(caseId)) { "deleted case must be absent from list" }
    }

    @Test
    fun `존재하지 않는 케이스 수정시 404 반환`() {
        mockMvc.put("/admin/redteam/cases/rt_case_notfound") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "없는 케이스"}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `빈 title로 케이스 등록시 400 반환`() {
        val orgId = uniqueOrgId()

        mockMvc.post("/admin/redteam/cases") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "$orgId",
                  "category": "out_of_domain",
                  "title": "",
                  "queryText": "오늘 날씨 어때?",
                  "expectedBehavior": "detect"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `일괄 실행 - RAG 미설정시 케이스별 SKIP 처리되고 배치런 완료`() {
        val orgId = uniqueOrgId()
        createCaseAndReturnId(orgId, category = "out_of_domain", expectedBehavior = "detect")

        val runResponse = mockMvc.post("/admin/redteam/batch-runs") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"organizationId": "$orgId"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("completed") }
        }.andReturn()
        val totalCases = runResponse.response.contentAsString.contentAsJson().path("totalCases").asInt()
        assert(totalCases > 0) { "batch run should include at least one active global case" }

        val runId = runResponse.response.contentAsString.contentAsJson().path("id").asText()

        mockMvc.get("/admin/redteam/batch-runs?organizationId=$orgId")
            .andExpect {
                status { isOk() }
                jsonPath("$.total") { value(1) }
            }

        mockMvc.get("/admin/redteam/batch-runs/$runId")
            .andExpect {
                status { isOk() }
                jsonPath("$.run.id") { value(runId) }
                jsonPath("$.results") { isArray() }
            }
    }

    @Test
    fun `전역 활성 케이스가 있으면 임의 기관으로도 일괄 실행된다`() {
        mockMvc.post("/admin/redteam/batch-runs") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"organizationId": "org_no_cases_xyz_${UUID.randomUUID().toString().substring(0, 4)}"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("completed") }
        }
    }

    @Test
    fun `배치런 이력 목록 조회`() {
        val orgId = uniqueOrgId()
        createCaseAndReturnId(orgId, category = "harmful_content", expectedBehavior = "defend")

        mockMvc.post("/admin/redteam/batch-runs") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"organizationId": "$orgId"}"""
        }.andExpect { status { isCreated() } }

        mockMvc.get("/admin/redteam/batch-runs?organizationId=$orgId")
            .andExpect {
                status { isOk() }
                jsonPath("$.runs") { isArray() }
                jsonPath("$.runs[0].status") { value("completed") }
            }
    }

    private fun createCaseAndReturnId(
        orgId: String,
        category: String = "pii_induction",
        expectedBehavior: String = "defend",
    ): String {
        val response = mockMvc.post("/admin/redteam/cases") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "organizationId": "$orgId",
                  "category": "$category",
                  "title": "테스트 케이스",
                  "queryText": "테스트 질의",
                  "expectedBehavior": "$expectedBehavior"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        return response.response.contentAsString.contentAsJson().path("id").asText()
    }
}
