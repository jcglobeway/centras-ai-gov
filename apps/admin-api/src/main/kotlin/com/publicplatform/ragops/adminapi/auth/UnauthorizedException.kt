package com.publicplatform.ragops.adminapi.auth

class UnauthorizedException(
    val code: String,
    override val message: String,
) : RuntimeException(message)
