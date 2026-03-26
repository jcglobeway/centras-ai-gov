# Status

- 상태: `implemented`
- 시작일: `2026-03-25`
- 마지막 업데이트: `2026-03-25`

## Progress

- Phase A~E 구현 완료
- `ChatRuntimeApiTests`, `RagasEvaluationApiTest`, `ArchitectureTest` 통과
- pre-existing 실패 10개 (auth/ingestion/e2e)는 이전 커밋부터 존재 — 이번 변경과 무관

## Verification

- `ChatRuntimeApiTests` PASS (5개)
- `ArchitectureTest` PASS (8개)
- `RagasEvaluationApiTest` PASS (3개)
- admin-api + rag-orchestrator 재시작 후 E2E 검증 필요

## Risks

- `CreateQuestionService` 생성자 변경 시 `ServiceConfiguration`에서 새 port 주입 누락 시 컨텍스트 로드 실패
- H2에서 `@Modifying` native query의 `Boolean` 파라미터 타입이 PostgreSQL과 다를 수 있음 → JPQL UPDATE로 대체 고려
- rag-orchestrator `confidence_score`가 pgvector cosine distance 기준이므로 값이 항상 0~1은 아닐 수 있음 (음수 가능) → `max(0.0, 1 - distance)` 적용 필요
