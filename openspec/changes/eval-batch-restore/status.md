# Status

- 상태: `done`
- 시작일: `2026-04-03`
- 마지막 업데이트: `2026-04-03`

## Progress

- proposal.md, tasks.md, status.md 초안 작성 완료
- Kotlin 백엔드 전 레이어 구현 완료
  - `PatchRagasEvaluationPort` / `PatchRagasEvaluationUseCase` / `PatchRagasEvaluationService` / `PatchRagasEvaluationPortAdapter` (COALESCE SQL)
  - `GET /admin/ragas-evaluations/by-question/{questionId}` 엔드포인트 추가
  - `PATCH /admin/ragas-evaluations/by-question/{questionId}` 엔드포인트 추가
  - `RepositoryConfiguration` / `ServiceConfiguration` Bean 등록
- Python eval-runner 변경 완료
  - `AdminApiClient.get_evaluation()` / `patch_evaluation()` 추가
  - `_compute_missing_metrics()` 헬퍼 추가 (null 필드만 선택 계산)
  - `evaluate_batch()` PATCH 전략으로 중복 행 방지 로직 변경

## Verification

- RagasEvaluationApiTest: 7개 (기존 3 + 신규 4) — 전부 통과
- ArchUnit 8개 규칙 — 위반 없음
- 전체 테스트: 54개 통과, 0 failures

## Risks

- eval-runner 수동 실행 검증은 별도 환경(prod DB + Ollama)에서 수행 필요
