/**
 * 서비스 헬스체크 엔드포인트.
 *
 * 로드밸런서와 CI 파이프라인이 서비스 가용성을 확인할 때 사용한다.
 */
package com.publicplatform.ragops.adminapi.health.adapter.inbound.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/healthz")
class HealthController {
    @GetMapping
    fun health(): HealthResponse =
        HealthResponse(
            status = "ok",
            service = "admin-api",
            timestamp = Instant.now(),
        )
}

data class HealthResponse(
    val status: String,
    val service: String,
    val timestamp: Instant,
)
