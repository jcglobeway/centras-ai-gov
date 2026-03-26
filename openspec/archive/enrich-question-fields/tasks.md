# Tasks

## Phase A — questionCategory 전달 파이프라인

- [x] `CreateQuestionCommand`에 `questionCategory: String? = null` 추가 (`QuestionSummary.kt`)
- [x] `RecordQuestionPortAdapter.kt` — `questionCategory = command.questionCategory` (null 하드코딩 제거)
- [x] `QuestionController.kt` — `CreateQuestionRequest`에 `questionCategory: String?` 추가, `CreateQuestionCommand`에 전달
- [x] `query_runner.py` — POST payload에 `questionCategory` (`consulting_category`), `questionIntentLabel` (`task_category`) 추가

## Phase B — rag-orchestrator 신뢰도·실패코드 계산

- [x] `app.py` — `GenerateAnswerResponse`에 `confidence_score`, `question_failure_reason_code`, `is_escalated` 추가
- [x] `app.py` — `generate_answer()` 내부에서 pgvector distance 기반 confidence 계산 (1 - avg_distance)
- [x] `app.py` — zero-result → `question_failure_reason_code = "A04"`, `is_escalated = True` 룰 적용
- [x] `RagAnswerResult.kt` — `confidenceScore: BigDecimal?`, `questionFailureReasonCode: String?`, `isEscalated: Boolean` 추가
- [x] `RagOrchestratorClient.kt` — `GenerateAnswerResult`에 신규 필드 추가, `toRagAnswerResult()` 매핑 추가

## Phase C — UpdateQuestionPort 신규 생성

- [x] `UpdateQuestionPort.kt` 신규 인터페이스 생성
- [x] `JpaQuestionRepository.kt` — `updateEnrichment()` JPQL `@Modifying` query 추가
- [x] `UpdateQuestionPortAdapter.kt` 신규 어댑터 생성 (`open class`)

## Phase D — UpdateChatSessionPort 신규 생성

- [x] `UpdateChatSessionPort.kt` 신규 인터페이스 생성
- [x] `JpaChatSessionRepository.kt` 신규 — `incrementQuestionCount()`, `updateSessionEndType()` JPQL 쿼리
- [x] `UpdateChatSessionPortAdapter.kt` 신규 어댑터 생성 (`open class`)

## Phase E — CreateQuestionService 통합

- [x] `CreateQuestionService.kt` — 생성자에 `updateQuestionPort`, `updateChatSessionPort` 주입
- [x] `CreateQuestionService.kt` — 질문 생성 후 `incrementQuestionCount()` 호출
- [x] `CreateQuestionService.kt` — ragResult 수신 후 `updateAfterAnswer()` + `updateSessionEndType()` 호출
- [x] `RepositoryConfiguration.kt` — `UpdateQuestionPortAdapter`, `UpdateChatSessionPortAdapter` @Bean 등록
- [x] `ServiceConfiguration.kt` — `CreateQuestionService` 생성자 업데이트

## Phase F — 검증

- [x] `ChatRuntimeApiTests`, `RagasEvaluationApiTest`, `ArchitectureTest` 통과 확인 (pre-existing 실패 10개는 이전 커밋부터 존재)
- [ ] admin-api + rag-orchestrator 재시작 후 `query-runner --limit 5` 실행
- [ ] psql로 questions/chat_sessions 컬럼 값 확인
