# Tasks

## P1: 어댑터 파일명 ↔ 클래스명 일치 (20개 rename)

### ingestion-ops
- [ ] `CrawlSourceReaderAdapter.kt` → `LoadCrawlSourcePortAdapter.kt`
- [ ] `CrawlSourceWriterAdapter.kt` → `SaveCrawlSourcePortAdapter.kt`
- [ ] `IngestionJobReaderAdapter.kt` → `LoadIngestionJobPortAdapter.kt`
- [ ] `IngestionJobWriterAdapter.kt` → `PersistIngestionJobPortAdapter.kt`

### identity-access
- [ ] `AdminSessionRepositoryAdapter.kt` → `ManageAdminSessionPortAdapter.kt`
- [ ] `AdminUserRepositoryAdapter.kt` → `ManageAdminUserPortAdapter.kt`
- [ ] `AuditLogRepositoryAdapter.kt` → `RecordAuditLogPortAdapter.kt`

### organization-directory
- [ ] `OrganizationDirectoryReaderAdapter.kt` → `LoadOrganizationPortAdapter.kt`

### chat-runtime
- [ ] `QuestionReaderAdapter.kt` → `LoadQuestionPortAdapter.kt`
- [ ] `AnswerReaderAdapter.kt` → `LoadAnswerPortAdapter.kt`
- [ ] `AnswerWriterAdapter.kt` → `RecordAnswerPortAdapter.kt`
- [ ] `FeedbackReaderAdapter.kt` → `LoadFeedbackPortAdapter.kt`
- [ ] `FeedbackWriterAdapter.kt` → `RecordFeedbackPortAdapter.kt`
- [ ] `RagSearchLogWriterAdapter.kt` → `SaveRagSearchLogPortAdapter.kt`

### document-registry
- [ ] `DocumentReaderAdapter.kt` → `LoadDocumentPortAdapter.kt`
- [ ] `DocumentVersionReaderAdapter.kt` → `LoadDocumentVersionPortAdapter.kt`
- [ ] `DocumentWriterAdapter.kt` → `SaveDocumentPortAdapter.kt`

### metrics-reporting
- [ ] `MetricsReaderAdapter.kt` → `LoadMetricsPortAdapter.kt`
- [ ] `MetricsWriterAdapter.kt` → `SaveMetricsPortAdapter.kt`

### qa-review
- [ ] `QAReviewReaderAdapter.kt` → `LoadQAReviewPortAdapter.kt`
- [ ] `QAReviewWriterAdapter.kt` → `RecordQAReviewPortAdapter.kt`

## P2: port/out/ 개별 파일 분리

### chat-runtime (ChatRuntimePorts.kt → 7개 파일)
- [ ] `LoadQuestionPort.kt`
- [ ] `RecordQuestionPort.kt`
- [ ] `LoadAnswerPort.kt`
- [ ] `RecordAnswerPort.kt`
- [ ] `SaveRagSearchLogPort.kt`
- [ ] `LoadFeedbackPort.kt`
- [ ] `RecordFeedbackPort.kt`
- [ ] `RagOrchestrationPort.kt`
- [ ] `ChatRuntimePorts.kt` 삭제

### identity-access (IdentityAccessPorts.kt → 5개 파일)
- [ ] `RestoreSessionPort.kt`
- [ ] `ManageAdminSessionPort.kt`
- [ ] `AdminCredentialAuthenticator.kt`
- [ ] `ManageAdminUserPort.kt`
- [ ] `RecordAuditLogPort.kt`
- [ ] `IdentityAccessPorts.kt` 삭제

### ingestion-ops (IngestionOpsPorts.kt → 4개 파일)
- [ ] `LoadCrawlSourcePort.kt`
- [ ] `SaveCrawlSourcePort.kt`
- [ ] `LoadIngestionJobPort.kt`
- [ ] `PersistIngestionJobPort.kt`
- [ ] `IngestionOpsPorts.kt` 삭제

### organization-directory (OrganizationDirectoryPorts.kt → 1개 파일)
- [ ] `LoadOrganizationPort.kt`
- [ ] `OrganizationDirectoryPorts.kt` 삭제

### qa-review (QAReviewPorts.kt → 2개 파일)
- [ ] `LoadQAReviewPort.kt`
- [ ] `RecordQAReviewPort.kt`
- [ ] `QAReviewPorts.kt` 삭제

### document-registry (DocumentRegistryPorts.kt → 3개 파일)
- [ ] `LoadDocumentPort.kt`
- [ ] `LoadDocumentVersionPort.kt`
- [ ] `SaveDocumentPort.kt`
- [ ] `DocumentRegistryPorts.kt` 삭제

### metrics-reporting (MetricsReportingPorts.kt → 2개 파일)
- [ ] `LoadMetricsPort.kt`
- [ ] `SaveMetricsPort.kt`
- [ ] `MetricsReportingPorts.kt` 삭제

### 영향 import 일괄 수정
- [ ] 각 모듈의 Service, Adapter, 테스트 파일 import 수정

## P3: auth/ 레이어 구조 적용

- [ ] `AuthCommandController.kt` → `auth/adapter/inbound/web/` 이동
- [ ] `AuthMeController.kt` → `auth/adapter/inbound/web/` 이동
- [ ] `AuthExceptionHandler.kt` → `auth/adapter/inbound/web/` 이동
- [ ] package 선언 수정 및 import 수정

## P4: 어댑터 파일 위치 이동

- [ ] `chatruntime/RagOrchestratorClient.kt` → `chatruntime/adapter/outbound/http/RagOrchestratorClient.kt`
- [ ] `metrics/MetricsAggregationScheduler.kt` → `metrics/adapter/inbound/scheduler/MetricsAggregationScheduler.kt`
- [ ] `health/HealthController.kt` → `health/adapter/inbound/web/HealthController.kt`
- [ ] 각 파일 package 선언 수정, import 수정

## P5: *Domain.kt 타입별 파일 분리

### chat-runtime
- [ ] enum: `AnswerStatus.kt`, `FailureReasonCode.kt`
- [ ] domain models: `QuestionSummary.kt`, `AnswerSummary.kt`, `FeedbackSummary.kt`, `RagSearchLogSummary.kt`, `ChatSessionSummary.kt`
- [ ] commands: 도메인 커맨드 (현 위치 유지 — port.out 참조로 인해 domain에 위치)
- [ ] scope types: `ChatScope.kt`, `FeedbackScope.kt`
- [ ] `ChatRuntimeDomain.kt` 삭제

### ingestion-ops
- [ ] enum: `IngestionJobStatus.kt`, `IngestionJobStage.kt` 등 enum 파일
- [ ] `IngestionJobStateMachine.kt` (도메인 서비스)
- [ ] `IngestionOpsDomain.kt` 삭제

### 나머지 모듈 (qa-review, document-registry, identity-access, organization-directory, metrics-reporting)
- [ ] 모듈별 주요 타입을 파일로 분리

## 공통

- [ ] `./gradlew compileKotlin` 통과 확인
- [ ] `./gradlew test` 전체 통과 확인 (ArchUnit 포함)
- [ ] 커밋
