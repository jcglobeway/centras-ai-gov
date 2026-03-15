package com.publicplatform.ragops.adminapi.config

import com.publicplatform.ragops.identityaccess.*
import com.publicplatform.ragops.organizationdirectory.*
import com.publicplatform.ragops.ingestionops.*
import com.publicplatform.ragops.qareview.*
import com.publicplatform.ragops.chatruntime.*
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

    @Bean
    fun crawlSourceReader(
        jpaRepository: JpaCrawlSourceRepository,
    ): CrawlSourceReader {
        return CrawlSourceReaderAdapter(jpaRepository)
    }

    @Bean
    fun crawlSourceWriter(
        jpaRepository: JpaCrawlSourceRepository,
    ): CrawlSourceWriter {
        return CrawlSourceWriterAdapter(jpaRepository)
    }

    @Bean
    fun ingestionJobReader(
        jpaRepository: JpaIngestionJobRepository,
    ): IngestionJobReader {
        return IngestionJobReaderAdapter(jpaRepository)
    }

    @Bean
    fun ingestionJobWriter(
        jpaIngestionJobRepository: JpaIngestionJobRepository,
        jpaCrawlSourceRepository: JpaCrawlSourceRepository,
    ): IngestionJobWriter {
        return IngestionJobWriterAdapter(jpaIngestionJobRepository, jpaCrawlSourceRepository)
    }

    @Bean
    fun qaReviewReader(
        jpaRepository: JpaQAReviewRepository,
    ): QAReviewReader {
        return QAReviewReaderAdapter(jpaRepository)
    }

    @Bean
    fun qaReviewWriter(
        jpaRepository: JpaQAReviewRepository,
    ): QAReviewWriter {
        return QAReviewWriterAdapter(jpaRepository)
    }

    @Bean
    fun questionReader(
        jpaRepository: JpaQuestionRepository,
    ): QuestionReader {
        return QuestionReaderAdapter(jpaRepository)
    }

    @Bean
    fun questionWriter(
        jpaRepository: JpaQuestionRepository,
    ): QuestionWriter {
        return QuestionWriterAdapter(jpaRepository)
    }

    @Bean
    fun answerReader(
        jpaRepository: JpaAnswerRepository,
    ): AnswerReader {
        return AnswerReaderAdapter(jpaRepository)
    }

    @Bean
    fun answerWriter(
        jpaRepository: JpaAnswerRepository,
    ): AnswerWriter {
        return AnswerWriterAdapter(jpaRepository)
    }
}
