# Tasks

> **Note**: 이 변경의 모든 deliverable은 `introduce-spring-ai`를 통해 구현 완료됨.
> SSE 스트리밍은 Spring AI 네이티브 방식으로, RAGAS 평가는 별도 eval-runner 패키지로 처리.

## 선행 조건

- [x] `naming-structure-cleanup` 완료 및 패키지 위치 확정

## 1. DB 마이그레이션

- [x] `V025__create_ragas_evaluations.sql` 작성
- [x] H2 호환 확인 (`application-test.yml` `flyway.target` 갱신)

## 2. Admin-API — RAGAS 평가 수신 모듈 (헥사고날)

- [x] `RecordRagasEvaluationCommand.kt` (question_id, 4개 점수, eval_model)
- [x] `RecordRagasEvaluationUseCase.kt` (`application/port/in/`)
- [x] `RagasEvaluationService.kt` (`application/service/`)
- [x] `RagasEvaluationEntity.kt` + `JpaRagasEvaluationRepository.kt`
- [x] `RagasEvaluationController.kt` — `POST /admin/ragas-evaluations`
- [x] `ServiceConfiguration.kt` Bean 등록
- [x] `RepositoryConfiguration.kt` 어댑터 Bean 등록

## 3. rag-orchestrator — SSE 스트리밍 + RAGAS 평가

- [x] `pyproject.toml` 의존성 추가: `ragas`, `datasets`
- [x] `app.py` — `/evaluate` 엔드포인트 (Spring AI가 스트리밍 담당, 평가는 별도 호출)
- [x] `app.py` — RAGAS 평가 로직 (Faithfulness, Answer Relevance, Context Precision)
- [x] 기존 `POST /generate` 동기 엔드포인트 유지

## 4. Admin-API — 스트리밍 프록시

- [x] `SpringAiAnswerService.kt` — Spring AI 네이티브 스트리밍 (WebClient 대신)
- [x] `QuestionStreamController.kt` — `GET /admin/questions/stream` SSE 엔드포인트

## 5. eval-runner (신규 Python 패키지)

- [x] `python/eval-runner/pyproject.toml` — `ragas`, `pandas`, `httpx`, `typer`
- [x] `python/eval-runner/ragas_batch.py` — GT 없음 모드 (3개 지표)
- [x] `python/eval-runner/query_runner.py` — RAG 질의 + eval_results.json 생성

## 6. 설정

- [x] `application.yml` — `spring.ai.ollama.*` 설정 블록 추가

## 7. 테스트

- [x] 기존 47개 테스트 전체 통과
- [x] `RagasEvaluationApiTest` 신규 작성 — POST 수신 + DB 저장 검증 (3개)
- [x] 전체 50개 테스트 BUILD SUCCESSFUL

## 8. 문서 / 마무리

- [x] `openspec/changes/rag-pipeline-v2-streaming-ragas/status.md` 업데이트
- [x] 커밋 (introduce-spring-ai 커밋으로 반영)
- [x] 아카이브
