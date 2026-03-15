package com.publicplatform.ragops.adminapi.auth

import com.publicplatform.ragops.identityaccess.AdminRoleAssignment
import com.publicplatform.ragops.identityaccess.AdminSessionReader
import com.publicplatform.ragops.identityaccess.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.AdminUser
import com.publicplatform.ragops.identityaccess.AdminUserStatus
import com.publicplatform.ragops.identityaccess.SessionLookup
import com.publicplatform.ragops.organizationdirectory.OrganizationDirectoryReader
import com.publicplatform.ragops.organizationdirectory.OrganizationSummary
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DevelopmentAdminSessionReader(
    private val organizationDirectoryReader: OrganizationDirectoryReader,
) : AdminSessionReader {
    override fun restoreSession(lookup: SessionLookup): AdminSessionSnapshot {
        val roleCode = lookup.roleCodeHint?.ifBlank { null } ?: "ops_admin"
        val organizationId = lookup.organizationIdHint?.ifBlank { null } ?: defaultOrganizationFor(roleCode)

        val roleAssignments = listOf(
            AdminRoleAssignment(
                roleCode = roleCode,
                organizationId = organizationId,
            ),
        )

        val knownOrganizationIds = organizationDirectoryReader
            .getOrganizations(roleAssignments.mapNotNull { it.organizationId }.toSet())
            .map { it.id }
            .toSet()

        val normalizedRoleAssignments = roleAssignments.map { assignment ->
            if (assignment.organizationId == null || assignment.organizationId in knownOrganizationIds) {
                assignment
            } else {
                assignment.copy(organizationId = null)
            }
        }

        return AdminSessionSnapshot(
            user = AdminUser(
                id = lookup.userIdHint?.ifBlank { null } ?: "usr_dev_ops_001",
                email = lookup.emailHint?.ifBlank { null } ?: "ops.admin@gov-platform.kr",
                displayName = lookup.displayNameHint?.ifBlank { null } ?: "Operations Admin",
                status = AdminUserStatus.ACTIVE,
                lastLoginAt = Instant.parse("2026-03-15T09:00:00Z"),
            ),
            roleAssignments = normalizedRoleAssignments,
            grantedActions = actionsFor(roleCode),
        )
    }

    private fun actionsFor(roleCode: String): List<String> =
        when (roleCode) {
            "ops_admin" -> listOf(
                "dashboard.read",
                "organization.read",
                "organization.update",
                "crawl_source.read",
                "crawl_source.write",
                "ingestion_job.read",
                "document.read",
                "document.reingest.request",
                "document.reindex.request",
                "document.reindex.execute",
                "qa.review.read",
                "qa.review.write",
                "metrics.read",
                "auth.user.read",
                "auth.role.assign",
            )

            "qa_admin" -> listOf(
                "dashboard.read",
                "crawl_source.read",
                "ingestion_job.read",
                "document.read",
                "document.reindex.request",
                "qa.review.read",
                "qa.review.write",
                "metrics.read",
            )

            "client_admin" -> listOf(
                "dashboard.read",
                "crawl_source.read",
                "ingestion_job.read",
                "document.read",
                "document.reingest.request",
                "document.reindex.request",
                "metrics.read",
            )

            else -> listOf("dashboard.read")
        }

    private fun defaultOrganizationFor(roleCode: String): String? =
        when (roleCode) {
            "ops_admin" -> null
            else -> "org_seoul_120"
        }
}

@Service
class InMemoryOrganizationDirectoryReader : OrganizationDirectoryReader {
    private val organizations = listOf(
        OrganizationSummary(
            id = "org_seoul_120",
            name = "Seoul City Civil Service Center",
            institutionType = "metro",
        ),
        OrganizationSummary(
            id = "org_busan_220",
            name = "Busan Citizen Support Center",
            institutionType = "metro",
        ),
    )

    override fun getOrganizations(ids: Set<String>): List<OrganizationSummary> =
        organizations.filter { it.id in ids }
}
