# Tasks

## Phase A — failure_reason_code 강화

- [x] `app.py` — confidence < 0.4 → `question_failure_reason_code = "A05"`, `is_escalated = True`
- [x] `app.py` — `get_embedding()` 호출 후 `query_embedding` 반환 (retrieval.py 기존 함수 재사용)
- [x] `GenerateAnswerResponse` — `query_embedding: Optional[List[float]]` 추가

## Phase B — Flyway 마이그레이션

- [x] `V029__add_question_embedding.sql` — `ALTER TABLE questions ADD COLUMN question_embedding TEXT`
- [x] `V030__QuestionEmbeddingVector.kt` — PG only: vector(1024) 타입 변환 + HNSW 인덱스 (H2 no-op)
- [x] `application-test.yml` — `flyway.target: "29"`

## Phase C — QuestionEntity + Port + Adapter 확장

- [x] `QuestionEntity.kt` — `questionEmbedding: String?` 추가
- [x] `UpdateQuestionPort.kt` — `updateEmbedding(questionId, embedding)` 추가
- [x] `JpaQuestionRepository.kt` — `updateEmbedding()` JPQL + `findFaqCandidates()` native query 추가
- [x] `FaqCandidateProjection.kt` 신규 (persistence 패키지)
- [x] `UpdateQuestionPortAdapter.kt` — `updateEmbedding()` 구현 추가

## Phase D+E — 도메인 + HTTP 클라이언트

- [x] `RagAnswerResult.kt` — `queryEmbedding: List<Float>?` 추가
- [x] `RagOrchestratorClient.kt` — `query_embedding` 매핑 추가

## Phase F — CreateQuestionService 임베딩 저장

- [x] `CreateQuestionService.kt` — ragResult.queryEmbedding 수신 후 `updateEmbedding()` 호출

## Phase G — FAQ 후보 엔드포인트

- [x] `FaqCandidate.kt` 신규 도메인 모델
- [x] `ListFaqCandidatesUseCase.kt` 신규 (port/in)
- [x] `LoadFaqCandidatesPort.kt` 신규 (port/out)
- [x] `LoadFaqCandidatesPortAdapter.kt` 신규 (adapter/outbound/persistence)
- [x] `ListFaqCandidatesService.kt` 신규 (application/service)
- [x] `RepositoryConfiguration.kt` — `loadFaqCandidatesPort` @Bean 등록
- [x] `ServiceConfiguration.kt` — `listFaqCandidatesUseCase` @Bean 등록
- [x] `QuestionController.kt` — `GET /admin/faq-candidates` + `FaqCandidateListResponse` 추가

## Phase H — 검증

- [x] `./gradlew test` 통과 확인 (50개, pre-existing 10개 실패 동일)
- [x] rag-orchestrator 재시작 후 query_embedding 응답 확인
- [x] DB에서 question_embedding 채워진 것 확인 (벡터 1024차원 저장 확인)
- [ ] FAQ 후보 엔드포인트 응답 확인 (PostgreSQL 환경, 데이터 충분히 쌓인 후)
