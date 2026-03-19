package com.publicplatform.ragops.adminapi.auth

/**
 * 인증되지 않은 요청에 대해 발생하는 예외.
 *
 * AdminRequestSessionResolver에서 세션이 없거나 만료된 경우 throw하며,
 * AuthExceptionHandler가 이를 잡아 401/403 응답으로 변환한다.
 * code 필드는 표준화된 실패 원인 코드(A01~A10)를 담는다.
 */
class UnauthorizedException(
    val code: String,
    override val message: String,
) : RuntimeException(message)
