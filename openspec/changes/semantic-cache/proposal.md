# Proposal

## Change ID

`semantic-cache`

## Summary

- **변경 목적**: cache-hit-rate의 exact-match 캐시 위에 **semantic cache 레이어**를 추가한다. 동일 질문 재입력(exact-match)뿐 아니라 의미상 동일한 다양한 표현("주차요금이 얼마예요" / "주차비 알려줘")에도 캐시 HIT가 발생하도록 한다.
- **변경 범위**:
  - DB (V044): `question_semantic_cache` 테이블 신규 (pgvector embedding + cached answer JSON)
  - `rag-orchestrator`: `/generate` 핸들러에 semantic cache 레이어 삽입 (exact MISS → semantic lookup → full pipeline)
  - `_log_search_result()`: `cache_hit=True` 유지 (semantic HIT도 동일하게 기록)
- **제외 범위**:
  - langchain RedisSemanticCache 미사용 (langchain 미도입 방침 유지)
  - Admin UI에서 semantic cache 항목 조회/삭제 기능 미포함
  - 캐시 무효화(TTL 만료 외) 미포함

## Impact

- **영향 모듈**: `python/rag-orchestrator`, DB migration
- **영향 API**: 기존 `POST /generate` 내부 동작만 변경 (인터페이스 변화 없음)
- **영향 테이블**: 신규 `question_semantic_cache`
- **영향 테스트**: 기존 50개 통과 유지 (V044는 flyway.target "29" 이후)

## Problem

| 항목 | 현재 | 목표 |
|------|------|------|
| "주차요금이 얼마예요" 1회차 | MISS | MISS (신규 → full pipeline + 캐시 저장) |
| "주차요금이 얼마예요" 2회차 | HIT (exact) | HIT (exact) |
| "주차비 알려줘" | MISS | HIT (semantic ≥ 0.92) |
| "parking fee?" | MISS | HIT or MISS (유사도에 따라) |

## Proposed Solution

```
/generate 요청
  │
  ├─ [HIT] Layer 1: Redis exact-match (기존)
  │         → cache_hit=True 기록, 즉시 반환
  │
  ├─ [MISS] Layer 2: pgvector semantic lookup (신규)
  │         → get_embedding(question) → question_semantic_cache cosine search
  │         → similarity >= SEMANTIC_CACHE_THRESHOLD(0.92): HIT
  │             → cache_hit=True 기록, 즉시 반환
  │             → Redis exact-match에도 저장 (이후 exact HIT 가능)
  │
  └─ [MISS] Layer 3: 기존 full pipeline (pgvector + LLM)
            → 완료 후 Redis exact-match 저장 (기존)
            → question_semantic_cache에 embedding + answer 저장 (신규)
            → cache_hit=False 기록
```

### DB 스키마 (V044)

```sql
CREATE TABLE question_semantic_cache (
    id              VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    question_text   TEXT NOT NULL,
    embedding_vector vector(1024),
    cached_answer   JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_hit_at     TIMESTAMP,
    hit_count       INT NOT NULL DEFAULT 0
);

CREATE INDEX ON question_semantic_cache
    USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 10);
```

### 환경변수

```
SEMANTIC_CACHE_ENABLED=true        # 기본값 true
SEMANTIC_CACHE_THRESHOLD=0.92      # cosine similarity 임계값 (0~1)
SEMANTIC_CACHE_MAX_RESULTS=1       # 최근접 1개만 조회
```

### 캐시 키 / 저장 로직

- 저장 시: `id = "qsc_{uuid8}"`, `embedding_vector = get_embedding(normalized_question)`
- 조회 SQL:
  ```sql
  SELECT id, cached_answer, 1 - (embedding_vector <=> %s::vector) AS similarity
  FROM question_semantic_cache
  WHERE organization_id = %s
    AND 1 - (embedding_vector <=> %s::vector) >= %s
  ORDER BY similarity DESC
  LIMIT 1
  ```
- HIT 시: `hit_count + 1`, `last_hit_at = NOW()` UPDATE

## Done Definition

1. "주차요금이 얼마예요" 1회 → full pipeline + 양쪽 캐시 저장
2. "주차비 알려줘" → semantic HIT (유사도 ≥ 0.92) → `cache_hit = true` DB 기록
3. `question_semantic_cache` 테이블에 embedding + answer JSON 저장 확인
4. 기존 Redis exact-match 캐시 동작 유지
5. 기존 백엔드 테스트 50개 전원 통과
