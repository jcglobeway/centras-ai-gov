package com.publicplatform.ragops.adminapi.health

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
