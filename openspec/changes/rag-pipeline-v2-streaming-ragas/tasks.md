# Tasks

## 선행 조건

- [ ] `naming-structure-cleanup` 완료 및 패키지 위치 확정

## 1. DB 마이그레이션

- [ ] `V025__create_ragas_evaluations.sql` 작성
- [ ] H2 호환 확인 (`application-test.yml` `flyway.target` 갱신)

## 2. Admin-API — RAGAS 평가 수신 모듈 (헥사고날)

- [ ] `RecordRagasEvaluationCommand.kt` (question_id, 4개 점수, eval_model)
- [ ] `RecordRagasEvaluationUseCase.kt` (`application/port/in/`)
- [ ] `RagasEvaluationService.kt` (`application/service/`)
- [ ] `RagasEvaluationEntity.kt` + `JpaRagasEvaluationRepository.kt`
- [ ] `RagasEvaluationController.kt` — `POST /admin/ragas-evaluations`
- [ ] `ServiceConfiguration.kt` Bean 등록
- [ ] `RepositoryConfiguration.kt` 어댑터 Bean 등록

## 3. rag-orchestrator — SSE 스트리밍 + RAGAS 평가

- [ ] `pyproject.toml` 의존성 추가: `sse-starlette>=1.6.0`, `ragas>=0.1.0`, `datasets`
- [ ] `app.py` — `POST /generate/stream` SSE 엔드포인트 (`EventSourceResponse`)
- [ ] `app.py` — `evaluate_with_ragas()` BackgroundTask 함수
  - Faithfulness, Answer Relevance, Context Precision 3개 지표
  - `POST /admin/ragas-evaluations` 콜백 (best-effort, 실패 무시)
- [ ] 기존 `POST /generate` 동기 엔드포인트 유지

## 4. Admin-API — 스트리밍 프록시

- [ ] `build.gradle.kts` — `spring-boot-starter-webflux` 추가 (WebClient 목적)
- [ ] `RagOrchestratorClient.kt` — `streamAnswer()` 메서드 추가 (WebClient SSE 수신)
- [ ] `QuestionController.kt` (또는 별도 `QuestionStreamController`) — `GET /admin/questions/stream` SSE 엔드포인트

## 5. eval-runner (신규 Python 패키지)

- [ ] `python/eval-runner/pyproject.toml` — `ragas`, `pandas`, `httpx`, `typer`
- [ ] `python/eval-runner/ragas_batch.py` — GT 없음 모드 (3개 지표)
  - `--date` 옵션으로 날짜 지정
  - `--ground-truth gt.csv` 옵션 플래그 (현재 비활성, `web2rag-poc` 연계 시 활성화)
  - 출력: `report_{date}.json`

## 6. 설정

- [ ] `application.yml` — `evaluation.ragas.*`, `evaluation.llm-judge.*` 설정 블록 추가
- [ ] `.env.example` 환경변수 문서화 (RAGAS_EVAL_ENABLED 등 5개)

## 7. 테스트

- [ ] 기존 44개 테스트 전체 통과 (`./gradlew test`)
- [ ] `RagasEvaluationApiTest` 신규 작성 — POST 수신 + DB 저장 검증
- [ ] rag-orchestrator 스트리밍 수동 검증 (`curl -N`)
- [ ] admin-api 스트리밍 프록시 수동 검증

## 8. 문서 / 마무리

- [ ] `openspec/changes/rag-pipeline-v2-streaming-ragas/status.md` 업데이트
- [ ] 커밋 (Korean commit message)
- [ ] `openspec archive rag-pipeline-v2-streaming-ragas`
