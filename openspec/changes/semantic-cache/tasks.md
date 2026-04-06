# Tasks

## Phase 1 — DB 마이그레이션

- [x] `V044__create_question_semantic_cache.sql` 작성
  - `question_semantic_cache` 테이블 생성
  - `ivfflat` 인덱스 (lists=10, vector_cosine_ops)

## Phase 2 — rag-orchestrator: semantic cache 레이어

- [x] `app.py` — `_semantic_cache_lookup(embedding, org_id, conn_str) -> dict | None` 헬퍼 추가
- [x] `app.py` — `_semantic_cache_store(question_text, embedding, answer_dict, org_id, conn_str)` 헬퍼 추가
- [x] `app.py` — `/generate` 핸들러에 Layer 2 삽입 (exact MISS 후 semantic lookup)
- [x] semantic HIT 시 Redis exact-match에도 저장 (이후 exact HIT 가능하도록)
- [x] full pipeline MISS 후 `_semantic_cache_store()` 호출
- [x] `.env.example`에 `SEMANTIC_CACHE_ENABLED`, `SEMANTIC_CACHE_THRESHOLD`, `SEMANTIC_CACHE_MAX_RESULTS` 추가

## 최종 확인

- [x] "주차요금이 얼마예요" 1회차 → `question_semantic_cache` 행 저장 확인
- [x] "주차비 알려줘" → `cache_hit = true` DB 기록 확인
- [x] 기존 Redis exact-match 정상 동작 유지 확인
- [x] 백엔드 테스트 BUILD SUCCESSFUL (50개 통과)
- [ ] 커밋: `기능: semantic-cache — pgvector 기반 시맨틱 캐시 레이어`