# Tasks

## Phase 1 — export_db_eval_data.py 구현

- [x] `export_db_eval_data.py` 작성
  - DB에서 미평가 질문 조회 (ragas_evaluations 없는 것만)
  - `GET /admin/questions/{id}/context` → retrieved_chunks 수집
  - `eval_results.json` 형식으로 출력
- [x] `pyproject.toml` entry point 추가 (`export-db-eval-data`)
- [x] `ragas_batch.py` — `--from-eval-results` 플래그 추가
  - `fetch_questions_by_ids()`: question_id IN (...) 직접 조회 (날짜 필터 없음)
  - `eval_results.json`의 question_id → eval_by_id 매핑으로 contexts 보완

## Phase 2 — 실행

```bash
cd python/eval-runner
source .venv/bin/activate

# DB → eval_results.json (미평가 200건)
export-db-eval-data --org-id org_acc --limit 200

# RAGAS 평가 (faith + relevancy, eval_results.json question_id 기반)
eval-runner --from-eval-results --organization-id org_acc
```

- [ ] `export-db-eval-data` 실행 → eval_results.json 생성 확인
- [ ] `eval-runner --from-eval-results` 실행 완료

## Phase 3 — 검증

- [ ] ragas_evaluations 건수 확인 (482건 추가 여부)
- [ ] `/ops/quality` RAGAS 추세 차트 데이터 증가 확인
