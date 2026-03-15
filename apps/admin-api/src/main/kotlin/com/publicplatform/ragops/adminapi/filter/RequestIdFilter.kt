package com.publicplatform.ragops.adminapi.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * 모든 요청에 request_id와 trace_id를 자동 생성하여 MDC에 추가.
 * 로그 추적성 확보.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = request.getHeader("X-Request-ID") ?: generateRequestId()
        val traceId = request.getHeader("X-Trace-ID") ?: requestId

        // MDC에 추가 (로그에서 사용)
        MDC.put("request_id", requestId)
        MDC.put("trace_id", traceId)

        // Response header에 추가
        response.addHeader("X-Request-ID", requestId)
        response.addHeader("X-Trace-ID", traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }

    private fun generateRequestId(): String {
        return "req_${UUID.randomUUID().toString().substring(0, 12)}"
    }
}
