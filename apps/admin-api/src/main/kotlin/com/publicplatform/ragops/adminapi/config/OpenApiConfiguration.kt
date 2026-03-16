/**
 * OpenAPI(Swagger) 문서 설정.
 *
 * springdoc-openapi를 사용하며, /swagger-ui.html에서 접근 가능하다.
 * API 스펙은 관리자 전용이므로 인증 헤더(X-Admin-Session-Id) 정보를 포함한다.
 */
package com.publicplatform.ragops.adminapi.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun openApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Public RAG Ops Platform Admin API")
                .version("0.1.0")
                .description("공공기관 RAG 챗봇 운영 플랫폼 관리자 API"),
        )
}
