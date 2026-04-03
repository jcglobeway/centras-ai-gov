# Proposal

## Change ID

`eval-batch-restore`

## Summary

- **변경 목적**: eval-runner batch가 realtime-eval이 저장한 `ragas_evaluations` 행을 중복 INSERT하지 않고, null 필드만 선택적으로 채우도록(PATCH) 변경한다. 이를 통해 `AVG()` 집계 왜곡 및 중복 행 문제를 해결한다.
- **변경 범위**:
  - Kotlin (Admin API): GET/PATCH by-question 엔드포인트 + 헥사고날 레이어 전체
  - Python (eval-runner): `ragas_batch.py` 로직 변경 — INSERT 대신 조회 후 PATCH
- **제외 범위**:
  - DB 스키마 변경 없음 (`question_id` UNIQUE 제약 추가 안 함)
  - realtime-eval 경로 변경 없음 (POST /admin/ragas-evaluations 기존 그대로)
  - 기존 중복 행 정리 마이그레이션 없음 (운영 환경 데이터는 별도 판단)

## Impact

- **영향 모듈**: `modules/metrics-reporting` (port/in, port/out, service, adapter)
- **영향 API**:
  - 신규: `GET /admin/ragas-evaluations/by-question/{questionId}`
  - 신규: `PATCH /admin/ragas-evaluations/by-question/{questionId}`
- **영향 테스트**: `evaluation/RagasEvaluationApiTest.kt` — GET/PATCH 케이스 추가
- **영향 Python**: `python/eval-runner/eval_runner/ragas_batch.py`

## Problem

`ragas_evaluations` 테이블에 `question_id` UNIQUE 제약이 없어, 두 경로가 모두 INSERT를 수행하면 동일 질문에 대한 행이 중복 생성된다.

| 경로 | 지표 | 문제 |
|------|------|------|
| realtime-eval (Redis BRPOP) | faithfulness, answer_relevancy, citation_coverage, citation_correctness | context_precision/recall = null |
| eval-runner batch (수동 CLI) | 6지표 전체 재계산 후 INSERT | 중복 행 → AVG 왜곡 |

추가로, realtime-eval이 Ollama 오류로 faithfulness 등을 null로 저장해도 batch가 복원하지 못하는 문제가 있다.

## Proposed Solution

eval-runner batch 경로를 다음과 같이 변경한다.

```
1. GET /admin/ragas-evaluations/by-question/{questionId} 로 기존 행 조회
2. 행 없음  → 6지표 전체 계산 → POST (기존과 동일)
3. 행 있음  → null 필드만 선택 계산 → PATCH /admin/ragas-evaluations/by-question/{questionId}
             PATCH SQL: UPDATE SET col = COALESCE(새값, 기존값)
             → 이미 채워진 필드는 절대 덮어쓰지 않음
```

### 헥사고날 레이어 추가 목록

| 레이어 | 클래스 |
|--------|--------|
| port/out | `PatchRagasEvaluationPort` |
| port/in | `PatchRagasEvaluationUseCase`, `PatchRagasEvaluationCommand` |
| service | `PatchRagasEvaluationService` |
| adapter/outbound/persistence | `PatchRagasEvaluationPortAdapter` (JdbcTemplate, COALESCE SQL) |
| adapter/inbound/web | `RagasEvaluationController` 확장 |

> 기존에 생성된 `PatchRagasContextMetrics*` 파일들은 내용을 위 설계로 교체한다.

## Done Definition

### 기능 정상 동작 기준

1. `GET /admin/ragas-evaluations/by-question/{questionId}` 가 존재하는 행을 반환하고, 없으면 404를 반환한다.
2. `PATCH /admin/ragas-evaluations/by-question/{questionId}` 가 null 필드만 채우고, 이미 값이 있는 필드는 변경하지 않는다.
3. eval-runner batch 실행 후 동일 `question_id` 에 대해 `ragas_evaluations` 행이 1개를 초과하지 않는다.
4. realtime-eval이 저장한 faithfulness 값이 batch 실행 후에도 유지된다.

### 테스트 기준

- `RagasEvaluationApiTest` 에 다음 케이스 추가:
  - `GET by-question/{id}` — 존재하는 question_id → 200 + 행 반환
  - `GET by-question/{id}` — 없는 question_id → 404
  - `PATCH by-question/{id}` — null 필드 채우기 → 200 + 기존 값 유지 확인
  - `PATCH by-question/{id}` — 없는 question_id → 404
- 기존 3개 테스트 (`POST /admin/ragas-evaluations`) 계속 통과
- ArchUnit 8개 규칙 위반 없음
