package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.domain.AdminRoleAssignment
import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.AdminUser
import com.publicplatform.ragops.identityaccess.domain.AdminUserStatus
import com.publicplatform.ragops.identityaccess.domain.AuthenticatedAdminPrincipal
import com.publicplatform.ragops.identityaccess.application.port.out.AdminCredentialAuthenticator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * 개발 환경용 하드코딩된 자격 증명 인증기.
 * bcrypt로 비밀번호를 해싱하여 저장 (lazy 초기화로 성능 개선).
 */
@Service
class DevelopmentAdminCredentialAuthenticator(
    private val passwordEncoder: PasswordEncoder,
) : AdminCredentialAuthenticator {
    // lazy 초기화로 Bean 생성 시 즉시 해싱하지 않음 (성능 개선)
    private val accounts by lazy {
        mapOf(
            "ops@jcg.com" to DevelopmentAdminAccount(
                passwordHash = passwordEncoder.encode("pass1234"),
                snapshot = snapshotFor(
                    userId = "usr_ops_global_001",
                    email = "ops@jcg.com",
                    displayName = "Platform Operator",
                    roleCode = "ops_admin",
                    organizationId = null,
                ),
            ),
            "super@jcg.com" to DevelopmentAdminAccount(
                passwordHash = passwordEncoder.encode("pass1234"),
                snapshot = snapshotFor(
                    userId = "usr_super_001",
                    email = "super@jcg.com",
                    displayName = "Super Admin",
                    roleCode = "super_admin",
                    organizationId = null,
                ),
            ),
            "client@jcg.com" to DevelopmentAdminAccount(
                passwordHash = passwordEncoder.encode("pass1234"),
                snapshot = snapshotFor(
                    userId = "usr_client_central_001",
                    email = "client@jcg.com",
                    displayName = "Client Admin",
                    roleCode = "client_admin",
                    organizationId = "org_central_gov",
                ),
            ),
            "viewer@jcg.com" to DevelopmentAdminAccount(
                passwordHash = passwordEncoder.encode("pass1234"),
                snapshot = snapshotFor(
                    userId = "usr_client_viewer_001",
                    email = "viewer@jcg.com",
                    displayName = "Client Viewer",
                    roleCode = "client_viewer",
                    organizationId = "org_acc",
                ),
            ),
            "qa@jcg.com" to DevelopmentAdminAccount(
                passwordHash = passwordEncoder.encode("pass1234"),
                snapshot = snapshotFor(
                    userId = "usr_qa_001",
                    email = "qa@jcg.com",
                    displayName = "QA Manager",
                    roleCode = "qa_admin",
                    organizationId = "org_local_gov",
                ),
            ),
            "editor@jcg.com" to DevelopmentAdminAccount(
                passwordHash = passwordEncoder.encode("pass1234"),
                snapshot = snapshotFor(
                    userId = "usr_knowledge_editor_001",
                    email = "editor@jcg.com",
                    displayName = "Knowledge Editor",
                    roleCode = "knowledge_editor",
                    organizationId = "org_local_gov",
                ),
            ),
        )
    }

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
