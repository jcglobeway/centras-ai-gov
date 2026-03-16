package com.publicplatform.ragops.adminapi.exception

import com.publicplatform.ragops.identityaccess.domain.AdminAuthErrorCode
import com.publicplatform.ragops.identityaccess.domain.AdminAuthenticationException
import com.publicplatform.ragops.identityaccess.domain.AdminAuthorizationException
import com.publicplatform.ragops.ingestionops.domain.InvalidIngestionJobTransitionException
import com.publicplatform.ragops.qareview.domain.InvalidQAReviewException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

/**
 * 전역 예외 처리기.
 * 통일된 에러 응답 포맷 제공.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(AdminAuthenticationException::class)
    fun handleAuthenticationException(ex: AdminAuthenticationException): ResponseEntity<ErrorResponse> {
        val status = when (ex.code) {
            AdminAuthErrorCode.AUTH_INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED
            AdminAuthErrorCode.AUTH_SESSION_EXPIRED -> HttpStatus.UNAUTHORIZED
            AdminAuthErrorCode.AUTH_SESSION_REVOKED -> HttpStatus.UNAUTHORIZED
            else -> HttpStatus.UNAUTHORIZED
        }

        logger.warn("Authentication failed: {}", ex.message)

        return ResponseEntity.status(status).body(
            ErrorResponse(
                error = ErrorDetail(
                    code = ex.code.name,
                    message = ex.message,
                    requestId = MDC.get("request_id"),
                ),
                generatedAt = Instant.now(),
            ),
        )
    }

    @ExceptionHandler(AdminAuthorizationException::class)
    fun handleAuthorizationException(ex: AdminAuthorizationException): ResponseEntity<ErrorResponse> {
        logger.warn("Authorization failed: {}", ex.message)

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(
                error = ErrorDetail(
                    code = "AUTH_FORBIDDEN",
                    message = ex.message,
                    requestId = MDC.get("request_id"),
                ),
                generatedAt = Instant.now(),
            ),
        )
    }

    @ExceptionHandler(InvalidIngestionJobTransitionException::class)
    fun handleInvalidJobTransition(ex: InvalidIngestionJobTransitionException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid job transition: {}", ex.message)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = ErrorDetail(
                    code = "INVALID_JOB_TRANSITION",
                    message = ex.message ?: "Invalid job transition",
                    requestId = MDC.get("request_id"),
                ),
                generatedAt = Instant.now(),
            ),
        )
    }

    @ExceptionHandler(InvalidQAReviewException::class)
    fun handleInvalidQAReview(ex: InvalidQAReviewException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid QA review: {}", ex.message)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = ErrorDetail(
                    code = "INVALID_QA_REVIEW",
                    message = ex.message ?: "Invalid QA review",
                    requestId = MDC.get("request_id"),
                ),
                generatedAt = Instant.now(),
            ),
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation failed: {}", errors)

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = ErrorDetail(
                    code = "VALIDATION_ERROR",
                    message = "Validation failed: ${errors.joinToString(", ")}",
                    requestId = MDC.get("request_id"),
                ),
                generatedAt = Instant.now(),
            ),
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        logger.warn("Response status exception: {}", ex.reason)

        return ResponseEntity.status(ex.statusCode).body(
            ErrorResponse(
                error = ErrorDetail(
                    code = ex.statusCode.toString(),
                    message = ex.reason ?: "Request failed",
                    requestId = MDC.get("request_id"),
                ),
                generatedAt = Instant.now(),
            ),
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                error = ErrorDetail(
                    code = "INTERNAL_SERVER_ERROR",
                    message = "An unexpected error occurred",
                    requestId = MDC.get("request_id"),
                ),
                generatedAt = Instant.now(),
            ),
        )
    }
}

data class ErrorResponse(
    val error: ErrorDetail,
    val generatedAt: Instant,
)

data class ErrorDetail(
    val code: String,
    val message: String,
    val requestId: String?,
)
