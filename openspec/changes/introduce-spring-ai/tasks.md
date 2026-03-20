# Tasks

## A. 롤백
- [x] RagOrchestratorClient.kt — WebClient import, webClient 필드 제거

## B. Spring AI 빌드/설정
- [x] build.gradle.kts — spring-ai-starter-model-ollama:1.0.0 추가, kotlin("plugin.jpa") 추가
- [x] application.yml — spring.ai.ollama.*, spring-ai.answer.enabled 추가

## C. RAGAS evaluation 모듈 (헥사고날)
- [x] V025__create_ragas_evaluations.sql
- [x] domain/RagasEvaluationSummary.kt
- [x] application/port/in/RecordRagasEvaluationCommand.kt
- [x] application/port/in/RecordRagasEvaluationUseCase.kt
- [x] application/port/out/SaveRagasEvaluationPort.kt
- [x] application/service/RagasEvaluationService.kt
- [x] adapter/inbound/web/RagasEvaluationController.kt
- [x] adapter/outbound/persistence/RagasEvaluationEntity.kt
- [x] adapter/outbound/persistence/JpaRagasEvaluationRepository.kt
- [x] adapter/outbound/persistence/SaveRagasEvaluationPortAdapter.kt
- [x] RepositoryConfiguration.kt — SaveRagasEvaluationPortAdapter Bean 등록
- [x] ServiceConfiguration.kt — RagasEvaluationService Bean 등록
- [x] AdminApiApplication.kt — @EnableJpaRepositories, @EntityScan에 evaluation 패키지 추가

## D. Spring AI 서비스
- [x] SpringAiAnswerService.kt (adminapi/chatruntime/adapter/outbound/ai/)
- [x] QuestionStreamController.kt (adminapi/chatruntime/adapter/inbound/web/)
- [x] ServiceConfiguration.kt — SpringAiAnswerService를 RagOrchestrationPort Bean으로 교체

## E. Python
- [x] rag-orchestrator/app.py — /evaluate 엔드포인트 추가
- [x] rag-orchestrator/pyproject.toml — ragas, datasets optional deps 추가
- [x] python/eval-runner/ — 신규 패키지 (pyproject.toml + ragas_batch.py)

## F. 테스트
- [x] 기존 47개 테스트 100% 통과 (IngestionApiTests total: 4로 수정 포함)
- [x] RagasEvaluationApiTest 신규 작성 (3개 테스트)
- [x] 전체 50개 테스트 BUILD SUCCESSFUL
- [x] 커밋
