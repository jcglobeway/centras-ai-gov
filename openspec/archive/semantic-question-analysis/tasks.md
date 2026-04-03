# Tasks: semantic-question-analysis

## Phase 0 — DB 마이그레이션 (PostgreSQL)

- [x] `V034__create_question_keyword_stats.sql` 생성
- [x] `V035__create_question_similarity_groups.sql` 생성

## Phase 1 — eval-runner: Provider 추상화

- [x] `src/eval_runner/embedding_provider.py` 생성
  - `EmbeddingProvider` Protocol
  - `OllamaEmbeddingProvider` (bge-m3, httpx /api/embed)
  - `OpenAIEmbeddingProvider` (openai lazy import, text-embedding-3-small)
  - `get_embedding_provider()` 팩토리
- [x] `src/eval_runner/llm_provider.py` 생성
  - `LLMProvider` Protocol
  - `OllamaLLMProvider` (httpx /api/generate)
  - `OpenAILLMProvider` (openai lazy import, gpt-4o-mini)
  - `get_llm_provider()` 팩토리
- [x] `pyproject.toml` — openai optional dep, numpy 추가
- [x] `.env.example` — `EMBEDDING_PROVIDER`, `LLM_PROVIDER` 추가

## Phase 2 — eval-runner: `cluster-questions` CLI

- [x] `src/eval_runner/cluster_questions.py` 생성
  - psycopg2 questions 조회
  - LLM 키워드 추출 (배치, JSON 파싱)
  - keyword_occurrences 카운트
  - bge-m3 임베딩 → 코사인 유사도 행렬
  - Stage 1: 후보 쌍 선별 (threshold 0.75)
  - Stage 2: LLM yes/no 검증
  - Union-Find 클러스터 구성
  - `question_keyword_stats` upsert
  - `question_similarity_groups` upsert
  - `--dry-run` 지원
- [x] `pyproject.toml` — `cluster-questions` 엔트리포인트 추가

## Phase 3 — 백엔드 API (Spring Boot / Kotlin)

- [x] `MetricsController.kt` — `GET /admin/metrics/semantic-keywords` 추가
- [x] `MetricsController.kt` — `GET /admin/metrics/semantic-similar-groups` 추가
- [x] `SemanticKeywordItem`, `SemanticKeywordsResponse` data class
- [x] `SemanticSimilarGroupItem`, `SemanticSimilarGroupsResponse` data class
- [x] 기존 50개 테스트 통과 확인

## Phase 4 — 프론트엔드

- [x] `SemanticKeywordItem`, `SemanticKeywordsResponse` 인터페이스
- [x] `SemanticSimilarGroupItem`, `SemanticSimilarGroupsResponse` 인터페이스
- [x] semantic-keywords SWR 훅
- [x] semantic-similar-groups SWR 훅
- [x] TOP 키워드 카드: whitespace 기반 → semantic API 교체 (빈 상태 안내 포함)
- [x] 의미적 유사 질문 클러스터 카드 신규 추가

## Phase 4b — 질문 유형 분포 (LLM 동적 분류)

- [x] `V036__create_question_type_stats.sql` 생성
  - `question_type_stats(id, run_date, organization_id, type_label, count, created_at)`
- [x] `cluster_questions.py` Phase 3 추가
  - `derive_question_types()` — LLM으로 기관 특화 유형 5~7개 동적 도출
  - `classify_questions()` — 배치 단위 LLM 분류 (30개씩, "번호: 유형" 파싱)
  - `upsert_type_stats()` — `question_type_stats` upsert
- [x] `MetricsController.kt` — `GET /admin/metrics/question-type-distribution` 추가 (DB 읽기, try/catch)
- [x] `QuestionTypeItem`, `QuestionTypeDistributionResponse` data class 추가
- [x] 프론트엔드 — `QuestionTypeDistributionResponse` 인터페이스, SWR 훅
- [x] 프론트엔드 — 질문 유형 분포 도넛 차트 (처리 유형 분포 옆 2-col 그리드)

## Phase 5 — 검증 (미실행)

- [ ] `cluster-questions --org-id org_acc --days 7 --dry-run` 실행
- [ ] `cluster-questions --org-id org_acc --days 7` 실제 적재
- [ ] `GET /admin/metrics/semantic-keywords` API 응답 확인
- [ ] `GET /admin/metrics/semantic-similar-groups` API 응답 확인
- [ ] `/ops/statistics` 브라우저 확인
- [ ] `EMBEDDING_PROVIDER=openai` Provider 교체 확인
