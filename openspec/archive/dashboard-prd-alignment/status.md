# Status: dashboard-prd-alignment

- 상태: `done`
- 시작일: `2026-03-22`
- 마지막 업데이트: `2026-03-23`

## Progress

- Phase 1~4 완료: 백엔드 DTO 확장, 프론트 타입·페이지·컴포넌트 수정, V028 시드 생성
- Phase 5 완료: reset_data.sql 생성, eval-runner E2E 실행, RAGAS 평가 결과 저장 확인

## Verification

- `./gradlew :apps:admin-api:test` 50/50 통과 (2026-03-22)
- eval-runner query_runner: 공공 Q&A 100건 투입 완료 (2026-03-23)
- ragas_batch: Ollama (qwen2.5:7b) 기반 RAGAS 평가 145건 저장 완료 (2026-03-23)
  - faithfulness: 0.0~1.0, answerRelevancy: 0.0~0.87 범위 확인
  - NaN → null 변환 처리 완료

## Completed
