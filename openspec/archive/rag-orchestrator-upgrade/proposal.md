# Proposal: rag-orchestrator-upgrade

## 요약

두 가지 작업을 병행한다.

1. **Spring AI dead code 제거**: `rag.orchestrator.enabled=true`로 `RagOrchestratorClient`만 사용 중이므로 `SpringAiAnswerService`, `QuestionStreamController`, 관련 의존성이 완전히 dead code 상태다. 제거해 빌드 의존성과 코드 복잡도를 줄인다.

2. **rag-orchestrator 엔터프라이즈 RAG 고도화**: 기존 단순 pgvector cosine search(top_k=3)를 Hybrid Search(벡터+BM25) + RRF 융합 + FlashRank 리랭킹 3단계 파이프라인으로 교체해 검색 정확도를 높인다. 아울러 Ollama 응답에서 LLM 메트릭(토큰 수, 모델명)을 추출해 `answers` 테이블의 미채워진 V026 컬럼을 채운다.

## 배경

- `answers.model_name`, `input_tokens`, `output_tokens` 등 V026 컬럼이 null 상태 — rag-orchestrator가 LLM 메트릭을 추출하지 않았기 때문
- pgvector cosine search만 사용해 BM25 키워드 매칭이 없어 한국어 어절 단위 검색 품질이 낮음
- Spring AI 의존성(`spring-ai-starter-model-ollama`, `spring-ai-starter-model-openai`, `webflux`)이 완전히 미사용

## 범위

### Part 1 — Spring AI Dead Code 제거

| 파일 | 변경 |
|------|------|
| `apps/admin-api/src/main/kotlin/.../ai/SpringAiAnswerService.kt` | 삭제 |
| `apps/admin-api/src/main/kotlin/.../web/QuestionStreamController.kt` | 삭제 |
| `apps/admin-api/build.gradle.kts` | spring-ai 의존성 2개 + webflux 제거 |
| `apps/admin-api/src/main/resources/application.yml` | spring.ai, spring-ai.answer 블록 제거 |
| `apps/admin-api/src/main/kotlin/.../config/ServiceConfiguration.kt` | ragOrchestrationPort 단순화, questionStreamController 제거 |

### Part 2 — rag-orchestrator 고도화

| 파일 | 변경 |
|------|------|
| `python/rag-orchestrator/pyproject.toml` | rank-bm25, flashrank 추가 |
| `python/rag-orchestrator/src/rag_orchestrator/retrieval.py` | hybrid_search 3단계 파이프라인 (bm25_search, rrf_fusion, rerank) |
| `python/rag-orchestrator/src/rag_orchestrator/app.py` | LLM 메트릭 추출, hybrid_search 사용, GenerateAnswerResponse 필드 추가 |
| `apps/admin-api/src/main/kotlin/.../http/RagOrchestratorClient.kt` | LLM 메트릭 5개 필드 매핑 |

## 검색 파이프라인

```
hybrid_search()
  ├── vector_search()   pgvector cosine similarity (top_k=10)
  ├── bm25_search()     rank-bm25 키워드 검색 (top_k=10)
  ├── rrf_fusion()      Reciprocal Rank Fusion (k=60)
  └── rerank()          FlashRank cross-encoder (RERANKER_ENABLED=true 시)
```

## 환경변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `HYBRID_SEARCH_TOP_K` | 10 | 각 검색기 후보 수 |
| `RERANKER_ENABLED` | false | FlashRank cross-encoder 활성화 |
