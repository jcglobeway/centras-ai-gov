package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.AdminAuthErrorCode
import com.publicplatform.ragops.identityaccess.AdminAuthenticationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(AdminAuthenticationException::class)
    fun handleAuthenticationException(exception: AdminAuthenticationException): ResponseEntity<ErrorEnvelope> =
        ResponseEntity
            .status(exception.code.toHttpStatus())
            .body(
                ErrorEnvelope(
                    error = ErrorPayload(
                        code = exception.code.name,
                        message = exception.message,
                    ),
                ),
            )

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(exception: UnauthorizedException): ResponseEntity<ErrorEnvelope> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorEnvelope(
                    error = ErrorPayload(
                        code = exception.code,
                        message = exception.message,
                    ),
                ),
            )
}

private fun AdminAuthErrorCode.toHttpStatus(): HttpStatus =
    when (this) {
        AdminAuthErrorCode.AUTH_UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
        AdminAuthErrorCode.AUTH_INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED
        AdminAuthErrorCode.AUTH_SESSION_EXPIRED -> HttpStatus.UNAUTHORIZED
        AdminAuthErrorCode.AUTH_SESSION_REVOKED -> HttpStatus.UNAUTHORIZED
    }

data class ErrorEnvelope(
    val error: ErrorPayload,
)

data class ErrorPayload(
    val code: String,
    val message: String,
)

class UnauthorizedException(
    val code: String,
    override val message: String,
) : RuntimeException(message)
