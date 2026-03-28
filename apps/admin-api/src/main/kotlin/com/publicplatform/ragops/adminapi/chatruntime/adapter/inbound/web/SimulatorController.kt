package com.publicplatform.ragops.adminapi.chatruntime.adapter.inbound.web

import com.publicplatform.ragops.adminapi.auth.AdminRequestSessionResolver
import com.publicplatform.ragops.chatruntime.application.port.out.CreateChatSessionPort
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/simulator")
class SimulatorController(
    private val adminRequestSessionResolver: AdminRequestSessionResolver,
    private val createChatSessionPort: CreateChatSessionPort,
) {
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSession(
        @Valid @RequestBody request: SimulatorSessionRequest,
        servletRequest: HttpServletRequest,
    ): SimulatorSessionResponse {
        adminRequestSessionResolver.resolve(servletRequest)
        val sessionId = createChatSessionPort.create(
            organizationId = request.organizationId,
            serviceId = request.serviceId,
            channel = "simulator",
        )
        return SimulatorSessionResponse(sessionId = sessionId)
    }
}

data class SimulatorSessionRequest(
    @field:NotBlank val organizationId: String,
    @field:NotBlank val serviceId: String,
)

data class SimulatorSessionResponse(val sessionId: String)
