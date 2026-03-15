package com.publicplatform.ragops.adminapi.config

import com.publicplatform.ragops.identityaccess.*
import com.publicplatform.ragops.organizationdirectory.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Repository 어댑터를 Spring Bean으로 등록한다.
 * JPA 기반 구현을 사용.
 */
@Configuration
class RepositoryConfiguration {

    @Bean
    fun adminSessionRepository(
        jpaRepository: JpaAdminSessionRepository,
    ): AdminSessionRepository {
        return AdminSessionRepositoryAdapter(jpaRepository)
    }

    @Bean
    fun adminUserRepository(
        jpaRepository: JpaAdminUserRepository,
    ): AdminUserRepository {
        return AdminUserRepositoryAdapter(jpaRepository)
    }

    @Bean
    fun auditLogRepository(
        jpaRepository: JpaAuditLogRepository,
    ): AuditLogRepository {
        return AuditLogRepositoryAdapter(jpaRepository)
    }

    @Bean
    fun organizationRepository(
        jpaRepository: JpaOrganizationRepository,
    ): OrganizationRepository {
        return OrganizationRepositoryAdapter(jpaRepository)
    }

    @Bean
    fun serviceRepository(
        jpaRepository: JpaServiceRepository,
    ): ServiceRepository {
        return ServiceRepositoryAdapter(jpaRepository)
    }

    @Bean
    fun organizationDirectoryReader(
        jpaRepository: JpaOrganizationRepository,
    ): OrganizationDirectoryReader {
        return OrganizationDirectoryReaderAdapter(jpaRepository)
    }
}
