# Status

- 상태: `implemented`
- 시작일: `2026-04-04`
- 마지막 업데이트: `2026-04-06`

## Progress

- V044 마이그레이션 완료 (`question_semantic_cache` 테이블 + ivfflat 인덱스)
- `_semantic_cache_lookup` / `_semantic_cache_store` 헬퍼 구현 완료
- `/generate` 핸들러에 3-layer 캐시 구조 적용 (Redis → pgvector → LLM)
- semantic HIT 시 Redis에도 저장 (이후 exact HIT 가능)
- E2E 검증 완료 (hit_count 증가 확인)
- 백엔드 테스트 BUILD SUCCESSFUL
- `.env.example` 업데이트

## Risks

- `ivfflat` 인덱스는 행 수가 적을 때 효과 미미 (초기엔 sequential scan)
- pgvector 임베딩 생성 비용: Ollama bge-m3 호출 → exact MISS 시 항상 발생
- 임계값 0.90이 너무 엄격하면 HIT율 낮음, 너무 느슨하면 오답 반환 위험
