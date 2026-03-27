package com.publicplatform.ragops.adminapi.config

import com.publicplatform.ragops.chatruntime.application.port.out.RecordAnswerPort
import com.publicplatform.ragops.chatruntime.application.port.out.LoadFeedbackPort
import com.publicplatform.ragops.chatruntime.application.port.out.RecordFeedbackPort
import com.publicplatform.ragops.chatruntime.application.port.out.LoadLlmMetricsPort
import com.publicplatform.ragops.chatruntime.application.port.out.LoadQuestionPort
import com.publicplatform.ragops.chatruntime.application.port.out.RecordQuestionPort
import com.publicplatform.ragops.chatruntime.application.port.out.RagOrchestrationPort
import com.publicplatform.ragops.chatruntime.application.port.out.SaveRagSearchLogPort
import com.publicplatform.ragops.chatruntime.application.port.out.UpdateQuestionPort
import com.publicplatform.ragops.chatruntime.application.port.out.UpdateChatSessionPort
import com.publicplatform.ragops.chatruntime.application.port.out.LoadFaqCandidatesPort
import com.publicplatform.ragops.chatruntime.application.port.`in`.GetLlmMetricsUseCase
import com.publicplatform.ragops.chatruntime.application.port.`in`.ListFaqCandidatesUseCase
import com.publicplatform.ragops.chatruntime.application.service.CreateAnswerService
import com.publicplatform.ragops.chatruntime.application.service.CreateQuestionService
import com.publicplatform.ragops.chatruntime.application.service.GetLlmMetricsService
import com.publicplatform.ragops.chatruntime.application.service.ListFaqCandidatesService
import com.publicplatform.ragops.chatruntime.application.service.ListQuestionsService
import com.publicplatform.ragops.chatruntime.application.service.ManageFeedbackService
import com.publicplatform.ragops.chatruntime.application.service.SaveRagSearchLogService
import com.publicplatform.ragops.documentregistry.application.port.out.LoadDocumentPort
import com.publicplatform.ragops.documentregistry.application.port.out.LoadDocumentVersionPort
import com.publicplatform.ragops.documentregistry.application.port.out.SaveDocumentPort
import com.publicplatform.ragops.documentregistry.application.service.ListDocumentVersionsService
import com.publicplatform.ragops.documentregistry.application.service.ListDocumentsService
import com.publicplatform.ragops.documentregistry.application.service.SaveDocumentChunkService
import com.publicplatform.ragops.ingestionops.application.port.out.LoadCrawlSourcePort
import com.publicplatform.ragops.ingestionops.application.port.out.SaveCrawlSourcePort
import com.publicplatform.ragops.ingestionops.application.port.out.LoadIngestionJobPort
import com.publicplatform.ragops.ingestionops.application.port.out.PersistIngestionJobPort
import com.publicplatform.ragops.ingestionops.application.service.CreateCrawlSourceService
import com.publicplatform.ragops.ingestionops.application.service.ListIngestionService
import com.publicplatform.ragops.ingestionops.application.service.RunIngestionJobService
import com.publicplatform.ragops.ingestionops.application.service.TransitionJobService
import com.publicplatform.ragops.metricsreporting.application.port.out.LoadMetricsPort
import com.publicplatform.ragops.metricsreporting.application.port.out.SaveMetricsPort
import com.publicplatform.ragops.metricsreporting.application.service.ListMetricsService
import com.publicplatform.ragops.metricsreporting.application.service.UpsertDailyMetricsService
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.ListRagasEvaluationsUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.`in`.RecordRagasEvaluationUseCase
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.LoadRagasEvaluationsPort
import com.publicplatform.ragops.adminapi.evaluation.application.port.out.SaveRagasEvaluationPort
import com.publicplatform.ragops.adminapi.evaluation.application.service.ListRagasEvaluationsService
import com.publicplatform.ragops.adminapi.evaluation.application.service.RagasEvaluationService
import com.publicplatform.ragops.adminapi.chatruntime.adapter.outbound.http.RagOrchestratorClient
import org.springframework.boot.web.client.RestTemplateBuilder
import com.publicplatform.ragops.identityaccess.application.port.`in`.AdminAuthUseCase
import org.springframework.beans.factory.annotation.Value
import com.publicplatform.ragops.identityaccess.application.port.out.AdminCredentialAuthenticator
import com.publicplatform.ragops.identityaccess.application.port.out.ManageAdminSessionPort
import com.publicplatform.ragops.adminapi.auth.DevelopmentAdminSessionService
import com.publicplatform.ragops.adminapi.auth.DevelopmentRestoreSessionPort
import com.publicplatform.ragops.organizationdirectory.application.port.out.LoadOrganizationPort
import com.publicplatform.ragops.organizationdirectory.application.service.GetOrganizationsService
import com.publicplatform.ragops.qareview.application.port.out.LoadQAReviewPort
import com.publicplatform.ragops.qareview.application.port.out.RecordQAReviewPort
import com.publicplatform.ragops.qareview.application.service.CreateQAReviewService
import com.publicplatform.ragops.qareview.application.service.ListQAReviewsService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Application Service 구현체를 Spring Bean으로 등록한다.
 *
 * Service는 @Component를 사용하지 않으므로 여기서 명시적으로 등록한다.
 * 각 Service는 UseCase 인터페이스를 구현하며, outbound port에만 의존한다.
 */
@Configuration
class ServiceConfiguration {

    @Bean
    fun adminAuthUseCase(
        adminCredentialAuthenticator: AdminCredentialAuthenticator,
        adminSessionRepository: ManageAdminSessionPort,
        developmentRestoreSessionPort: DevelopmentRestoreSessionPort,
    ): AdminAuthUseCase = DevelopmentAdminSessionService(
        adminCredentialAuthenticator, adminSessionRepository, developmentRestoreSessionPort,
    )

    @Bean
    fun createQuestionService(
        questionWriter: RecordQuestionPort,
        answerWriter: RecordAnswerPort,
        ragOrchestrationPort: RagOrchestrationPort,
        updateQuestionPort: UpdateQuestionPort,
        updateChatSessionPort: UpdateChatSessionPort,
        eventPublisher: ApplicationEventPublisher,
    ): CreateQuestionService = CreateQuestionService(
        questionWriter, answerWriter, ragOrchestrationPort, updateQuestionPort, updateChatSessionPort, eventPublisher,
    )

    @Bean
    fun listQuestionsService(questionReader: LoadQuestionPort): ListQuestionsService =
        ListQuestionsService(questionReader)

    @Bean
    fun createAnswerService(answerWriter: RecordAnswerPort): CreateAnswerService =
        CreateAnswerService(answerWriter)

    @Bean
    fun manageFeedbackService(
        feedbackWriter: RecordFeedbackPort,
        feedbackReader: LoadFeedbackPort,
    ): ManageFeedbackService = ManageFeedbackService(feedbackWriter, feedbackReader)

    @Bean
    fun saveRagSearchLogService(ragSearchLogWriter: SaveRagSearchLogPort): SaveRagSearchLogService =
        SaveRagSearchLogService(ragSearchLogWriter)

    @Bean
    fun getOrganizationsService(organizationDirectoryReader: LoadOrganizationPort): GetOrganizationsService =
        GetOrganizationsService(organizationDirectoryReader)

    @Bean
    fun createCrawlSourceService(crawlSourceWriter: SaveCrawlSourcePort): CreateCrawlSourceService =
        CreateCrawlSourceService(crawlSourceWriter)

    @Bean
    fun runIngestionJobService(ingestionJobWriter: PersistIngestionJobPort): RunIngestionJobService =
        RunIngestionJobService(ingestionJobWriter)

    @Bean
    fun listIngestionService(
        crawlSourceReader: LoadCrawlSourcePort,
        ingestionJobReader: LoadIngestionJobPort,
    ): ListIngestionService = ListIngestionService(crawlSourceReader, ingestionJobReader)

    @Bean
    fun transitionJobService(
        ingestionJobWriter: PersistIngestionJobPort,
        eventPublisher: ApplicationEventPublisher,
    ): TransitionJobService = TransitionJobService(ingestionJobWriter, eventPublisher)

    @Bean
    fun createQAReviewService(
        qaReviewWriter: RecordQAReviewPort,
        eventPublisher: ApplicationEventPublisher,
    ): CreateQAReviewService = CreateQAReviewService(qaReviewWriter, eventPublisher)

    @Bean
    fun listQAReviewsService(qaReviewReader: LoadQAReviewPort): ListQAReviewsService =
        ListQAReviewsService(qaReviewReader)

    @Bean
    fun listDocumentsService(documentReader: LoadDocumentPort): ListDocumentsService =
        ListDocumentsService(documentReader)

    @Bean
    fun listDocumentVersionsService(documentVersionReader: LoadDocumentVersionPort): ListDocumentVersionsService =
        ListDocumentVersionsService(documentVersionReader)

    @Bean
    fun saveDocumentChunkService(documentWriter: SaveDocumentPort): SaveDocumentChunkService =
        SaveDocumentChunkService(documentWriter)

    @Bean
    fun listMetricsService(metricsReader: LoadMetricsPort): ListMetricsService =
        ListMetricsService(metricsReader)

    @Bean
    fun upsertDailyMetricsService(metricsWriter: SaveMetricsPort): UpsertDailyMetricsService =
        UpsertDailyMetricsService(metricsWriter)

    @Bean
    fun recordRagasEvaluationUseCase(saveRagasEvaluationPort: SaveRagasEvaluationPort): RecordRagasEvaluationUseCase =
        RagasEvaluationService(saveRagasEvaluationPort)

    @Bean
    fun listRagasEvaluationsUseCase(loadRagasEvaluationsPort: LoadRagasEvaluationsPort): ListRagasEvaluationsUseCase =
        ListRagasEvaluationsService(loadRagasEvaluationsPort)

    @Bean
    fun getLlmMetricsUseCase(loadLlmMetricsPort: LoadLlmMetricsPort): GetLlmMetricsUseCase =
        GetLlmMetricsService(loadLlmMetricsPort)

    @Bean
    fun listFaqCandidatesUseCase(loadFaqCandidatesPort: LoadFaqCandidatesPort): ListFaqCandidatesUseCase =
        ListFaqCandidatesService(loadFaqCandidatesPort)

    @Bean
    fun ragOrchestrationPort(
        @Value("\${rag.orchestrator.enabled:false}") ragOrchestratorEnabled: Boolean,
        @Value("\${rag.orchestrator.url:http://localhost:8090}") ragOrchestratorUrl: String,
        restTemplateBuilder: RestTemplateBuilder,
    ): RagOrchestrationPort {
        if (ragOrchestratorEnabled) {
            return RagOrchestratorClient(ragOrchestratorUrl, true, restTemplateBuilder)
        }
        return object : RagOrchestrationPort {
            override fun generateAnswer(questionId: String, questionText: String, organizationId: String, serviceId: String) = null
        }
    }
}
