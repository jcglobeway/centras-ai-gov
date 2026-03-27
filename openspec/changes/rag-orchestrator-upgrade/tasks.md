# Tasks: rag-orchestrator-upgrade

## Part 1 — Spring AI Dead Code 제거

- [x] `SpringAiAnswerService.kt` 삭제
- [x] `QuestionStreamController.kt` 삭제
- [x] `build.gradle.kts` — spring-ai-starter-model-ollama, spring-ai-starter-model-openai, webflux 제거
- [x] `application.yml` — spring.ai, spring-ai.answer 블록 제거
- [x] `ServiceConfiguration.kt` — questionStreamController Bean, resolveModel() 제거, ragOrchestrationPort 단순화

## Part 2 — rag-orchestrator 고도화

### 2-A. LLM 메트릭 추출

- [x] `generate_answer_with_ollama()` → dict 반환 (content, model, input_tokens, output_tokens)
- [x] Ollama 응답에서 `prompt_eval_count` (input), `eval_count` (output) 추출
- [x] `GenerateAnswerResponse`에 model_name, provider_name, input_tokens, output_tokens, total_tokens 추가
- [x] `generate_answer()` 내부에서 total_tokens 계산 및 Response 포함

### 2-B. RagOrchestratorClient 매핑

- [x] `GenerateAnswerResult`에 model_name, provider_name, input_tokens, output_tokens, total_tokens 추가
- [x] `toRagAnswerResult()`에서 5개 필드 매핑

### 2-C. Hybrid Search 파이프라인

- [x] `pyproject.toml` — rank-bm25>=0.2.2, flashrank>=0.2.0 추가
- [x] `bm25_search()` — DB 청크 로드 → BM25Okapi (공백 토크나이저) → 상위 top_k 반환
- [x] `rrf_fusion()` — score[doc] += 1/(k + rank), k=60
- [x] `rerank()` — FlashRank ms-marco-MiniLM-L-12-v2 cross-encoder
- [x] `hybrid_search()` — 3단계 파이프라인 통합, distance 필드 RRF score 기반 호환 변환

### 2-D. app.py 검색 호출 수정

- [x] `vector_search` → `hybrid_search` 교체
- [x] `HYBRID_SEARCH_TOP_K`, `RERANKER_ENABLED` 환경변수 적용

## 검증

- [x] Spring AI 제거 후 `./gradlew :apps:admin-api:compileKotlin` 통과
- [x] `./gradlew test` — 전체 테스트 50개 통과
- [x] `/generate` live 테스트 — citation_count:5, model_name:"qwen2.5:7b", input_tokens:2786, total_tokens:3161 확인
