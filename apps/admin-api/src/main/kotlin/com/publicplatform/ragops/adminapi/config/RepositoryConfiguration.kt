package com.publicplatform.ragops.adminapi.config

import com.publicplatform.ragops.identityaccess.adapter.outbound.persistence.*
import com.publicplatform.ragops.identityaccess.application.port.out.*
import com.publicplatform.ragops.organizationdirectory.adapter.outbound.persistence.*
import com.publicplatform.ragops.organizationdirectory.application.port.out.*
import com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence.JpaRagConfigRepository
import com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence.JpaRagConfigVersionRepository
import com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence.LoadRagConfigPortAdapter
import com.publicplatform.ragops.organizationdirectory.ragconfig.adapter.outbound.persistence.RecordRagConfigPortAdapter
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.out.LoadRagConfigPort
import com.publicplatform.ragops.organizationdirectory.ragconfig.application.port.out.RecordRagConfigPort
import com.publicplatform.ragops.ingestionops.adapter.outbound.persistence.*
import com.publicplatform.ragops.ingestionops.application.port.out.*
import com.publicplatform.ragops.qareview.adapter.outbound.persistence.*
import com.publicplatform.ragops.qareview.application.port.out.*
import com.publicplatform.ragops.qareview.adapter.outbound.persistence.UpdateQAReviewAssigneePortAdapter
import com.publicplatform.ragops.chatruntime.adapter.outbound.persistence.*
import com.publicplatform.ragops.chatruntime.application.port.out.*
import com.publicplatform.ragops.chatruntime.application.port.out.LoadCorrectionPort
import com.publicplatform.ragops.chatruntime.application.port.out.RecordCorrectionPort
import com.publicplatform.ragops.documentregistry.adapter.outbound.persistence.*
import com.publicplatform.ragops.documentregistry.application.port.out.*
import com.publicplatform.ragops.documentregistry.adapter.outbound.persistence.DeleteCollectionChunksPortAdapter
import com.publicplatform.ragops.documentregistry.adapter.outbound.persistence.SaveDocumentRecordPortAdapter
import com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence.*
import com.publicplatform.ragops.metricsreporting.application.port.out.*
import com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence.AnomalyThresholdPortAdapter
import com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence.AlertEventPortAdapter
import com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence.JpaAlertThresholdRepository
import com.publicplatform.ragops.metricsreporting.adapter.outbound.persistence.JpaAlertEventRepository
import com.publicplatform.ragops.adminapi.evaluation.adapter.inbound.event.RagasEvalQueuePublisher
import com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence.JpaRagasEvaluationRepository
import com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence.LoadRagasEvaluationsPortAdapter
import com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence.LoadRagasEvaluationSummaryPortAdapter
import com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence.PatchRagasEvaluationPortAdapter
import com.publicplatform.ragops.adminapi.evaluation.adapter.outbound.persistence.SaveRagasEvaluationPortAdapter
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.LoadRagasEvaluationsPort
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.LoadRagasEvaluationSummaryPort
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.PatchRagasEvaluationPort
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.SaveRagasEvaluationPort
import com.publicplatform.ragops.redteam.adapter.outbound.persistence.JpaRedteamCaseRepository
import com.publicplatform.ragops.redteam.adapter.outbound.persistence.JpaRedteamBatchRunRepository
import com.publicplatform.ragops.redteam.adapter.outbound.persistence.JpaRedteamCaseResultRepository
import com.publicplatform.ragops.redteam.adapter.outbound.persistence.RedteamCasePortAdapter
import com.publicplatform.ragops.redteam.adapter.outbound.persistence.RedteamBatchRunPortAdapter
import com.publicplatform.ragops.redteam.adapter.outbound.persistence.RedteamCaseResultPortAdapter
import com.publicplatform.ragops.redteam.application.port.out.LoadRedteamCasePort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamCasePort
import com.publicplatform.ragops.redteam.application.port.out.LoadRedteamBatchRunPort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamBatchRunPort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamCaseResultPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import com.publicplatform.ragops.qareview.adapter.inbound.event.QuestionAnsweredEventHandler
import com.publicplatform.ragops.qareview.application.port.`in`.CreateQAReviewUseCase
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
    fun loadAuditLogPort(jdbcTemplate: JdbcTemplate): LoadAuditLogPort =
        LoadAuditLogPortAdapter(jdbcTemplate)

    @Bean
    fun loadAdminUsersPort(jpaRepository: JpaManageAdminUserPort): LoadAdminUsersPort =
        LoadAdminUsersPortAdapter(jpaRepository)

    @Bean
    fun organizationDirectoryReader(
        jpaOrganizationRepository: JpaOrganizationRepository,
        jpaServiceRepository: JpaServiceRepository,
    ): LoadOrganizationPort =
        LoadOrganizationPortAdapter(jpaOrganizationRepository, jpaServiceRepository)

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
    fun updateQAReviewAssigneePort(jpaRepository: JpaQAReviewRepository): UpdateQAReviewAssigneePort =
        UpdateQAReviewAssigneePortAdapter(jpaRepository)

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
    fun documentWriter(
        jpaChunkRepository: JpaDocumentChunkRepository,
        jdbcTemplate: JdbcTemplate,
    ): SaveDocumentPort =
        SaveDocumentPortAdapter(jpaChunkRepository, jdbcTemplate)

    @Bean
    fun saveDocumentRecordPort(jpaDocumentRepository: JpaDocumentRepository): SaveDocumentRecordPort =
        SaveDocumentRecordPortAdapter(jpaDocumentRepository)

    @Bean
    fun deleteCollectionChunksPort(jdbcTemplate: JdbcTemplate): DeleteCollectionChunksPort =
        DeleteCollectionChunksPortAdapter(jdbcTemplate)

    @Bean
    fun ragSearchLogWriter(
        jpaSearchLogRepository: JpaRagSearchLogRepository,
        jpaRetrievedDocumentRepository: JpaRagRetrievedDocumentRepository,
    ): SaveRagSearchLogPort = SaveRagSearchLogPortAdapter(jpaSearchLogRepository, jpaRetrievedDocumentRepository)

    @Bean
    fun ragSearchLogReader(jpaRepository: JpaRagSearchLogRepository): com.publicplatform.ragops.chatruntime.application.port.out.LoadRagSearchLogPort =
        LoadRagSearchLogPortAdapter(jpaRepository)

    @Bean
    fun feedbackWriter(jpaRepository: JpaFeedbackRepository): RecordFeedbackPort =
        RecordFeedbackPortAdapter(jpaRepository)

    @Bean
    fun feedbackReader(jpaRepository: JpaFeedbackRepository): LoadFeedbackPort =
        LoadFeedbackPortAdapter(jpaRepository)

    @Bean
    fun correctionWriter(jpaRepository: JpaAnswerCorrectionRepository): RecordCorrectionPort =
        RecordCorrectionPortAdapter(jpaRepository)

    @Bean
    fun correctionReader(jpaRepository: JpaAnswerCorrectionRepository): LoadCorrectionPort =
        LoadCorrectionPortAdapter(jpaRepository)

    @Bean
    fun metricsWriter(jpaRepository: JpaDailyMetricsRepository): SaveMetricsPort =
        SaveMetricsPortAdapter(jpaRepository)

    @Bean
    fun saveRagasEvaluationPort(jpaRepository: JpaRagasEvaluationRepository): SaveRagasEvaluationPort =
        SaveRagasEvaluationPortAdapter(jpaRepository)

    @Bean
    fun loadRagasEvaluationsPort(jpaRepository: JpaRagasEvaluationRepository): LoadRagasEvaluationsPort =
        LoadRagasEvaluationsPortAdapter(jpaRepository)

    @Bean
    fun loadRagasEvaluationSummaryPort(jdbcTemplate: JdbcTemplate): LoadRagasEvaluationSummaryPort =
        LoadRagasEvaluationSummaryPortAdapter(jdbcTemplate)

    @Bean
    fun patchRagasEvaluationPort(jdbcTemplate: JdbcTemplate): PatchRagasEvaluationPort =
        PatchRagasEvaluationPortAdapter(jdbcTemplate)

    @Bean
    fun loadLlmMetricsPort(
        answerRepository: JpaAnswerRepository,
        questionRepository: JpaQuestionRepository,
    ): com.publicplatform.ragops.chatruntime.application.port.out.LoadLlmMetricsPort =
        LoadLlmMetricsPortAdapter(answerRepository, questionRepository)

    @Bean
    fun updateQuestionPort(jpaRepository: JpaQuestionRepository): UpdateQuestionPort =
        UpdateQuestionPortAdapter(jpaRepository)

    @Bean
    fun updateChatSessionPort(jpaRepository: JpaChatSessionRepository): UpdateChatSessionPort =
        UpdateChatSessionPortAdapter(jpaRepository)

    @Bean
    fun createChatSessionPort(jpaRepository: JpaChatSessionRepository): CreateChatSessionPort =
        CreateChatSessionPortAdapter(jpaRepository)

    @Bean
    fun loadChatSessionPort(jpaRepository: JpaChatSessionRepository): com.publicplatform.ragops.chatruntime.application.port.out.LoadChatSessionPort =
        LoadChatSessionPortAdapter(jpaRepository)

    @Bean
    fun loadFaqCandidatesPort(jpaRepository: JpaQuestionRepository): com.publicplatform.ragops.chatruntime.application.port.out.LoadFaqCandidatesPort =
        LoadFaqCandidatesPortAdapter(jpaRepository)

    @Bean
    fun questionAnsweredEventHandler(createQAReviewUseCase: CreateQAReviewUseCase): QuestionAnsweredEventHandler =
        QuestionAnsweredEventHandler(createQAReviewUseCase)

    @Bean
    fun loadRagConfigPort(
        jpaRagConfigRepository: JpaRagConfigRepository,
        jpaRagConfigVersionRepository: JpaRagConfigVersionRepository,
    ): LoadRagConfigPort = LoadRagConfigPortAdapter(jpaRagConfigRepository, jpaRagConfigVersionRepository)

    @Bean
    fun recordRagConfigPort(
        jpaRagConfigRepository: JpaRagConfigRepository,
        jpaRagConfigVersionRepository: JpaRagConfigVersionRepository,
    ): RecordRagConfigPort = RecordRagConfigPortAdapter(jpaRagConfigRepository, jpaRagConfigVersionRepository)

    @Bean
    fun loadAnomalyThresholdPort(jpaRepository: JpaAlertThresholdRepository): com.publicplatform.ragops.metricsreporting.application.port.out.LoadAnomalyThresholdPort =
        AnomalyThresholdPortAdapter(jpaRepository)

    @Bean
    fun saveAnomalyThresholdPort(jpaRepository: JpaAlertThresholdRepository): com.publicplatform.ragops.metricsreporting.application.port.out.SaveAnomalyThresholdPort =
        AnomalyThresholdPortAdapter(jpaRepository)

    @Bean
    fun loadAlertEventPort(jpaRepository: JpaAlertEventRepository): com.publicplatform.ragops.metricsreporting.application.port.out.LoadAlertEventPort =
        AlertEventPortAdapter(jpaRepository)

    @Bean
    fun saveAlertEventPort(jpaRepository: JpaAlertEventRepository): com.publicplatform.ragops.metricsreporting.application.port.out.SaveAlertEventPort =
        AlertEventPortAdapter(jpaRepository)

    @Bean
    fun redteamCasePortAdapter(jpaRepository: JpaRedteamCaseRepository): RedteamCasePortAdapter =
        RedteamCasePortAdapter(jpaRepository)

    @Bean
    fun loadRedteamCasePort(redteamCasePortAdapter: RedteamCasePortAdapter): LoadRedteamCasePort =
        redteamCasePortAdapter

    @Bean
    fun saveRedteamCasePort(redteamCasePortAdapter: RedteamCasePortAdapter): SaveRedteamCasePort =
        redteamCasePortAdapter

    @Bean
    fun redteamBatchRunPortAdapter(
        jpaRunRepository: JpaRedteamBatchRunRepository,
        jpaResultRepository: JpaRedteamCaseResultRepository,
    ): RedteamBatchRunPortAdapter = RedteamBatchRunPortAdapter(jpaRunRepository, jpaResultRepository)

    @Bean
    fun loadRedteamBatchRunPort(redteamBatchRunPortAdapter: RedteamBatchRunPortAdapter): LoadRedteamBatchRunPort =
        redteamBatchRunPortAdapter

    @Bean
    fun saveRedteamBatchRunPort(redteamBatchRunPortAdapter: RedteamBatchRunPortAdapter): SaveRedteamBatchRunPort =
        redteamBatchRunPortAdapter

    @Bean
    fun redteamCaseResultPortAdapter(jpaRepository: JpaRedteamCaseResultRepository): RedteamCaseResultPortAdapter =
        RedteamCaseResultPortAdapter(jpaRepository)

    @Bean
    fun saveRedteamCaseResultPort(redteamCaseResultPortAdapter: RedteamCaseResultPortAdapter): SaveRedteamCaseResultPort =
        redteamCaseResultPortAdapter

    @Bean
    @ConditionalOnBean(RedisTemplate::class)
    fun ragasEvalQueuePublisher(
        redisTemplate: RedisTemplate<String, String>,
        objectMapper: ObjectMapper,
    ): RagasEvalQueuePublisher = RagasEvalQueuePublisher(redisTemplate, objectMapper)
}
