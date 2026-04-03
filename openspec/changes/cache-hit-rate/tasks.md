# Tasks

## Phase 1 — rag-orchestrator: Redis 캐시 레이어

- [x] `pyproject.toml`에 `redis>=5.0` 의존성 추가
- [x] `app.py` — `_cache_key(question_text, org_id) -> str` 헬퍼 추가
- [x] `app.py` — `/generate` 핸들러에 캐시 조회 삽입 (HIT/MISS, 예외 폴백)
- [x] `_log_search_result()` 시그니처에 `cache_hit: bool = False` 추가
- [x] `.env.example`에 `RAG_CACHE_TTL_SEC=86400` 추가

## Phase 2 — DB 마이그레이션

- [x] `V043__add_cache_hit_to_rag_search_logs.sql` 작성 (`ALTER TABLE ... ADD COLUMN cache_hit BOOLEAN NOT NULL DEFAULT FALSE`)

## Phase 3 — Admin API: rag-search-logs 확장

- [x] `CreateRagSearchLogCommand`, `RagSearchLogSummary`에 `cacheHit: Boolean = false` 추가
- [x] `RagSearchLogEntity`에 `cache_hit` 컬럼 매핑 + `toSummary()` 추가
- [x] `RagSearchLogController` 요청 DTO + Command 전달
- [x] `SaveRagSearchLogPortAdapter`에서 `cacheHit` 저장

## Phase 4 — Admin API: cache-hit-trend 엔드포인트

- [x] `MetricsController.kt`에 `GET /admin/metrics/cache-hit-trend` 추가 (날짜별 GROUP BY, org 필터)
- [x] `CacheHitTrendItem`, `CacheHitTrendResponse` DTO 추가

## Phase 5 — 프론트엔드 (`/ops/cost/page.tsx`)

- [x] `CacheHitTrendResponse`, `CacheHitTrendItem` 타입 추가 (`src/lib/types.ts`)
- [x] SWR hook 추가: `GET /api/admin/metrics/cache-hit-trend?days=7&...`
- [x] Cache Hit Rate KPI 카드: 하드코딩 제거 → 실데이터 (ok ≥ 20%, warn ≥ 15%)
- [x] 추이 라인차트 카드 추가

## 최종 확인

- [ ] 동일 질문 2회 요청 → 두 번째 `cache_hit = true` DB 확인
- [ ] `GET /admin/metrics/cache-hit-trend` 응답 확인
- [ ] `/ops/cost` — 실데이터 KPI + 추이 차트 렌더링 확인
- [x] 백엔드 테스트 BUILD SUCCESSFUL (50개 통과)
- [ ] 커밋: `기능: cache-hit-rate — Redis 캐시 레이어 + 추이 대시보드`
