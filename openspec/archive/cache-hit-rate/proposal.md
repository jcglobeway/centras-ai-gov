# Proposal

## Change ID

`cache-hit-rate`

## Summary

- **변경 목적**: PRD v3 (2-4. 비용 추적) — Cache Hit Rate 지표를 실제 데이터로 수집·표시한다. 현재 `/ops/cost` 화면의 Cache Hit Rate 카드는 "23.4%" 하드코딩 + "구현 예정" 주석 상태다.
- **변경 범위**:
  - `rag-orchestrator`: question 정규화 텍스트 SHA256 해시 기반 Redis exact-match 캐시 레이어 추가. HIT 시 pgvector 검색 생략.
  - DB (V043): `rag_search_logs`에 `cache_hit BOOLEAN` 컬럼 추가.
  - Admin API: rag-search-logs POST에 `cacheHit` 필드 수신 + `GET /admin/metrics/cache-hit-trend` 엔드포인트 신규.
  - 프론트엔드: `/ops/cost` — Cache Hit Rate KPI 카드 실데이터 연결 + 추이 라인차트 추가.
- **제외 범위**:
  - langchain RedisSemanticCache 미사용 (rag-orchestrator가 langchain 미사용). Exact-match 우선, 시맨틱 캐시는 별도 change로.
  - 캐시 무효화(Invalidation) UI 미포함.
  - 기존 50개 백엔드 테스트 변경 없음 (V043은 H2 호환, flyway.target "29" 유지).

## Impact

- **영향 모듈**: `python/rag-orchestrator`, `apps/admin-api` (RagSearchLog 관련 레이어, MetricsController)
- **영향 API**:
  - 기존 확장: `POST /admin/rag-search-logs` — `cacheHit` 필드 추가
  - 신규: `GET /admin/metrics/cache-hit-trend?organization_id=&days=7`
- **영향 테이블**: `rag_search_logs` — `cache_hit BOOLEAN` 컬럼 추가 (V043)
- **영향 테스트**: 기존 50개 통과 유지. 신규 테스트 불필요 (JDBC 집계 쿼리는 기존 패턴).

## Problem

| 항목 | 현재 상태 | 목표 |
|------|----------|------|
| 캐시 레이어 | 없음 (매 질문 pgvector 검색) | Redis exact-match 캐시 |
| Cache Hit Rate KPI | 하드코딩 "23.4%" | 실 DB 집계값 |
| 추이 차트 | 없음 | 날짜별 Hit Rate 라인차트 |
| 알림 기준 | 없음 | < 15% Warning, < 5% Critical |

## Proposed Solution

```
질문 수신
  │
  ├─ [HIT] Redis GET(sha256(normalized_text)) → 캐시 응답 반환
  │         cache_hit=True 로 rag_search_logs 기록
  │         pgvector 검색 생략 → 응답 지연 감소
  │
  └─ [MISS] 기존 pgvector + LLM 파이프라인
            완료 후 Redis SET (TTL 24h)
            cache_hit=False 로 rag_search_logs 기록

MetricsController:
  GET /admin/metrics/cache-hit-trend
    → rag_search_logs 날짜별 집계 (hits / total = hit_rate)
```

캐시 키: `rag:cache:{organization_id}:{sha256(strip+lower+split join)}`

## Done Definition

1. 동일 질문 2회 연속 요청 시 두 번째 요청의 `cache_hit = true` 가 `rag_search_logs`에 기록된다.
2. `GET /admin/metrics/cache-hit-trend` 가 날짜별 hits / total / hit_rate 를 반환한다.
3. `/ops/cost` Cache Hit Rate KPI 카드가 실데이터를 표시하고, ok/warn/critical 상태를 반영한다.
4. 추이 라인차트가 최근 7일 hit rate를 시각화한다.
5. 기존 백엔드 테스트 50개 전원 통과.
