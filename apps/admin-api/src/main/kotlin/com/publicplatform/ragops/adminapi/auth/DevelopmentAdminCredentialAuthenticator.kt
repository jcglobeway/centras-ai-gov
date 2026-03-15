package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.AdminCredentialAuthenticator
import com.publicplatform.ragops.identityaccess.AdminRoleAssignment
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.AdminUser
import com.publicplatform.ragops.identityaccess.AdminUserStatus
import com.publicplatform.ragops.identityaccess.AuthenticatedAdminPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 개발 환경용 하드코딩된 자격 증명 인증기.
 * bcrypt로 비밀번호를 해싱하여 저장.
 */
@Service
class DevelopmentAdminCredentialAuthenticator(
    private val passwordEncoder: PasswordEncoder,
) : AdminCredentialAuthenticator {
    private val accounts = mapOf(
        "ops.platform@gov-platform.kr" to DevelopmentAdminAccount(
            passwordHash = passwordEncoder.encode("ops-pass-1234"),
            snapshot = snapshotFor(
                userId = "usr_ops_global_001",
                email = "ops.platform@gov-platform.kr",
                displayName = "Platform Operator",
                roleCode = "ops_admin",
                organizationId = null,
            ),
        ),
        "client.admin@busan.go.kr" to DevelopmentAdminAccount(
            passwordHash = passwordEncoder.encode("client-pass-1234"),
            snapshot = snapshotFor(
                userId = "usr_client_busan_001",
                email = "client.admin@busan.go.kr",
                displayName = "Busan Client Admin",
                roleCode = "client_admin",
                organizationId = "org_busan_220",
            ),
        ),
        "qa.manager@gov-platform.kr" to DevelopmentAdminAccount(
            passwordHash = passwordEncoder.encode("qa-pass-1234"),
            snapshot = snapshotFor(
                userId = "usr_qa_001",
                email = "qa.manager@gov-platform.kr",
                displayName = "QA Manager",
                roleCode = "qa_admin",
                organizationId = "org_seoul_120",
            ),
        ),
    )

    override fun authenticate(email: String, password: String): AuthenticatedAdminPrincipal? =
        accounts[email]
            ?.takeIf { passwordEncoder.matches(password, it.passwordHash) }
            ?.let { AuthenticatedAdminPrincipal(snapshot = it.snapshot) }

    private fun snapshotFor(
        userId: String,
        email: String,
        displayName: String,
        roleCode: String,
        organizationId: String?,
    ): AdminSessionSnapshot =
        AdminSessionSnapshot(
            user = AdminUser(
                id = userId,
                email = email,
                displayName = displayName,
                status = AdminUserStatus.ACTIVE,
                lastLoginAt = Instant.parse("2026-03-15T08:30:00Z"),
            ),
            roleAssignments = listOf(
                AdminRoleAssignment(
                    roleCode = roleCode,
                    organizationId = organizationId,
                ),
            ),
            grantedActions = developmentActionsFor(roleCode),
        )
}

private data class DevelopmentAdminAccount(
    val passwordHash: String,
    val snapshot: AdminSessionSnapshot,
)
