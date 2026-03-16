package com.publicplatform.ragops.adminapi.config

import com.publicplatform.ragops.identityaccess.adapter.outbound.persistence.*
import com.publicplatform.ragops.identityaccess.application.port.out.*
import com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence.*
import com.publicplatform.ragops.organizationdirectory.application.port.out.*
import com.publicplatform.ragops.ingestionops.adapter.outbound.persistence.*
import com.publicplatform.ragops.ingestionops.application.port.out.*
import com.publicplatform.ragops.qareview.adapter.outbound.persistence.*
import com.publicplatform.ragops.qareview.application.port.out.*
import com.publicplatform.ragops.chatruntime.adapter.outbound.persistence.*
import com.publicplatform.ragops.chatruntime.application.port.out.*
import com.publicplatform.ragops.documentregistry.adapter.outbound.persistence.*
import com.publicplatform.ragops.documentregistry.application.port.out.*
import com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence.*
import com.publicplatform.ragops.metricsreporting.application.port.out.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Repository 어댑터를 Spring Bean으로 등록한다.
 * JPA 기반 구현을 사용.
 */
@Configuration
class RepositoryConfiguration {

    @Bean
    fun adminSessionRepository(jpaRepository: JpaManageAdminSessionPort): ManageAdminSessionPort =
        ManageAdminSessionPortAdapter(jpaRepository)

    @Bean
    fun adminUserRepository(jpaRepository: JpaManageAdminUserPort): ManageAdminUserPort =
        ManageAdminUserPortAdapter(jpaRepository)

    @Bean
    fun auditLogRepository(jpaRepository: JpaRecordAuditLogPort): RecordAuditLogPort =
        RecordAuditLogPortAdapter(jpaRepository)

    @Bean
    fun organizationRepository(jpaRepository: JpaOrganizationRepository): OrganizationRepository =
        OrganizationRepositoryAdapter(jpaRepository)

    @Bean
    fun serviceRepository(jpaRepository: JpaServiceRepository): ServiceRepository =
        ServiceRepositoryAdapter(jpaRepository)

    @Bean
    fun organizationDirectoryReader(jpaRepository: JpaOrganizationRepository): LoadOrganizationPort =
        LoadOrganizationPortAdapter(jpaRepository)

    @Bean
    fun crawlSourceReader(jpaRepository: JpaCrawlSourceRepository): LoadCrawlSourcePort =
        LoadCrawlSourcePortAdapter(jpaRepository)

    @Bean
    fun crawlSourceWriter(jpaRepository: JpaCrawlSourceRepository): SaveCrawlSourcePort =
        SaveCrawlSourcePortAdapter(jpaRepository)

    @Bean
    fun ingestionJobReader(jpaRepository: JpaIngestionJobRepository): LoadIngestionJobPort =
        LoadIngestionJobPortAdapter(jpaRepository)

    @Bean
    fun ingestionJobWriter(
        jpaIngestionJobRepository: JpaIngestionJobRepository,
        jpaCrawlSourceRepository: JpaCrawlSourceRepository,
    ): PersistIngestionJobPort = PersistIngestionJobPortAdapter(jpaIngestionJobRepository, jpaCrawlSourceRepository)

    @Bean
    fun qaReviewReader(jpaRepository: JpaQAReviewRepository): LoadQAReviewPort =
        LoadQAReviewPortAdapter(jpaRepository)

    @Bean
    fun qaReviewWriter(jpaRepository: JpaQAReviewRepository): RecordQAReviewPort =
        RecordQAReviewPortAdapter(jpaRepository)

    @Bean
    fun questionReader(jpaRepository: JpaQuestionRepository): LoadQuestionPort =
        LoadQuestionPortAdapter(jpaRepository)

    @Bean
    fun questionWriter(jpaRepository: JpaQuestionRepository): RecordQuestionPort =
        RecordQuestionPortAdapter(jpaRepository)

    @Bean
    fun answerReader(jpaRepository: JpaAnswerRepository): LoadAnswerPort =
        LoadAnswerPortAdapter(jpaRepository)

    @Bean
    fun answerWriter(jpaRepository: JpaAnswerRepository): RecordAnswerPort =
        RecordAnswerPortAdapter(jpaRepository)

    @Bean
    fun documentReader(jpaRepository: JpaDocumentRepository): LoadDocumentPort =
        LoadDocumentPortAdapter(jpaRepository)

    @Bean
    fun documentVersionReader(jpaRepository: JpaDocumentVersionRepository): LoadDocumentVersionPort =
        LoadDocumentVersionPortAdapter(jpaRepository)

    @Bean
    fun metricsReader(jpaRepository: JpaDailyMetricsRepository): LoadMetricsPort =
        LoadMetricsPortAdapter(jpaRepository)

    @Bean
    fun documentWriter(jpaChunkRepository: JpaDocumentChunkRepository): SaveDocumentPort =
        SaveDocumentPortAdapter(jpaChunkRepository)

    @Bean
    fun ragSearchLogWriter(
        jpaSearchLogRepository: JpaRagSearchLogRepository,
        jpaRetrievedDocumentRepository: JpaRagRetrievedDocumentRepository,
    ): SaveRagSearchLogPort = SaveRagSearchLogPortAdapter(jpaSearchLogRepository, jpaRetrievedDocumentRepository)

    @Bean
    fun feedbackWriter(jpaRepository: JpaFeedbackRepository): RecordFeedbackPort =
        RecordFeedbackPortAdapter(jpaRepository)

    @Bean
    fun feedbackReader(jpaRepository: JpaFeedbackRepository): LoadFeedbackPort =
        LoadFeedbackPortAdapter(jpaRepository)

    @Bean
    fun metricsWriter(jpaRepository: JpaDailyMetricsRepository): SaveMetricsPort =
        SaveMetricsPortAdapter(jpaRepository)
}
