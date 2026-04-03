# Proposal

## Change ID

`citation-metrics`

## Summary

- **목적**: RAG 평가 지표에 Citation Coverage + Citation Correctness 2개 추가
- **범위**: DB 스키마(V038) → 백엔드 Kotlin 5계층 → eval-runner Python → 프론트엔드 2페이지
- **제외**: rag-orchestrator 변경 없음 (eval-runner에서 LLM judge로 계산)

**지표 정의**:
- **Citation Coverage** (목표 ≥ 80%): 검색된 N개 context 청크 중 LLM이 답변 생성에 실제로 활용한 비율. 낮으면 검색 효율 문제를 시사.
- **Citation Correctness** (목표 ≥ 85%): 활용된 청크들이 실제 답변 내용을 올바르게 지지하는 비율. Faithfulness와 방향 유사하지만 청크 단위 일치도 측정.

## Impact

- **영향 모듈**: `adminapi.evaluation` (domain/port/service/adapter/controller)
- **영향 API**: `POST /admin/ragas-evaluations`, `GET /admin/ragas-evaluations`, `GET /admin/ragas-evaluations/summary`
- **영향 테스트**: `RagasEvaluationApiTest` (3개 테스트 — 두 필드 포함 payload로 검증)

## Done Definition

- [ ] V038 마이그레이션 적용 후 H2 테스트 통과
- [ ] `POST /admin/ragas-evaluations`에 두 필드 저장 확인
- [ ] eval-runner `--dry-run` 출력에 `citationCoverage`, `citationCorrectness` 포함
- [ ] `/ops/quality` RAGAS 스코어카드 4행 → 6행 표시
- [ ] `/ops/quality-summary` 레이더 차트 4각형 → 6각형, 스코어 바 6개
